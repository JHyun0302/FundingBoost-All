package kcs.funding.fundingboost.saga.inbox;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InboxEventService {

    private final InboxEventRepository inboxEventRepository;

    @Transactional
    public boolean consumeOnce(
            String consumerName,
            String topic,
            String eventKey,
            String eventType,
            String payload,
            InboxWork inboxWork
    ) {
        InboxEvent inboxEvent = inboxEventRepository
                .findByConsumerNameAndTopicAndEventKey(consumerName, topic, eventKey)
                .orElseGet(() -> InboxEvent.received(consumerName, topic, eventKey, eventType, payload));

        if (inboxEvent.isCompleted()) {
            return false;
        }

        inboxEvent.prepareForRetry(payload);
        inboxEventRepository.save(inboxEvent);

        try {
            inboxWork.execute();
            inboxEvent.markCompleted();
            return true;
        } catch (RuntimeException exception) {
            inboxEvent.markFailed(exception.getMessage());
            throw exception;
        }
    }

    @FunctionalInterface
    public interface InboxWork {
        void execute();
    }
}
