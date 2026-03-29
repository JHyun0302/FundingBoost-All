package kcs.funding.fundingboost.event.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import kcs.funding.fundingboost.event.domain.OutboxEvent;
import kcs.funding.fundingboost.event.domain.OutboxEventRepository;
import kcs.funding.fundingboost.event.domain.OutboxEventStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.outbox", name = "enabled", havingValue = "true", matchIfMissing = true)
public class OutboxRelayService {

    private static final List<OutboxEventStatus> RELAY_STATUSES = List.of(
            OutboxEventStatus.PENDING,
            OutboxEventStatus.RETRY
    );

    private final OutboxEventRepository outboxEventRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;

    @Value("${app.outbox.batch-size:100}")
    private int batchSize;

    @Value("${app.outbox.max-retry:10}")
    private int maxRetry;

    @Value("${app.outbox.base-retry-seconds:5}")
    private int baseRetrySeconds;

    @Scheduled(fixedDelayString = "${app.outbox.relay-delay-ms:3000}")
    public void relayPendingEvents() {
        List<OutboxEvent> candidates = outboxEventRepository.findByStatusInAndNextAttemptAtLessThanEqualOrderByOutboxEventIdAsc(
                RELAY_STATUSES,
                LocalDateTime.now(),
                PageRequest.of(0, Math.max(batchSize, 1))
        );
        if (candidates.isEmpty()) {
            return;
        }
        for (OutboxEvent candidate : candidates) {
            transactionTemplate.executeWithoutResult(status -> relaySingle(candidate.getOutboxEventId()));
        }
    }

    public void relaySingle(Long outboxEventId) {
        OutboxEvent event = outboxEventRepository.findById(outboxEventId).orElse(null);
        if (event == null) {
            return;
        }

        LocalDateTime now = LocalDateTime.now();
        if (!event.isReadyAt(now)) {
            return;
        }

        Map<String, String> headers = readHeaders(event.getHeaders());
        headers.putIfAbsent("eventType", event.getEventType());
        headers.putIfAbsent("aggregateType", event.getAggregateType());
        headers.putIfAbsent("aggregateId", event.getAggregateId());

        try {
            domainEventPublisher.publish(event.getTopic(), event.getEventKey(), event.getPayload(), headers);
            event.markPublished(now);
        } catch (Exception exception) {
            event.markRetry(maxRetry, baseRetrySeconds, now, exception.getMessage());
            log.warn(
                    "outbox relay failed: id={}, topic={}, status={}, retry={}",
                    event.getOutboxEventId(),
                    event.getTopic(),
                    event.getStatus(),
                    event.getRetryCount()
            );
        }
        outboxEventRepository.save(event);
    }

    private Map<String, String> readHeaders(String headersJson) {
        if (headersJson == null || headersJson.isBlank()) {
            return new HashMap<>();
        }
        try {
            return new HashMap<>(objectMapper.readValue(headersJson, new TypeReference<Map<String, String>>() {
            }));
        } catch (Exception exception) {
            return new HashMap<>();
        }
    }
}
