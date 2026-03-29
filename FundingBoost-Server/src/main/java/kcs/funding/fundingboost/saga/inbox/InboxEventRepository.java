package kcs.funding.fundingboost.saga.inbox;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InboxEventRepository extends JpaRepository<InboxEvent, Long> {

    Optional<InboxEvent> findByConsumerNameAndTopicAndEventKey(String consumerName, String topic, String eventKey);
}
