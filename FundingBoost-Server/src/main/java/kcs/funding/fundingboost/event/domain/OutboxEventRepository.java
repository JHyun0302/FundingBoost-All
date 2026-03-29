package kcs.funding.fundingboost.event.domain;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {

    boolean existsByEventTypeAndEventKey(String eventType, String eventKey);

    List<OutboxEvent> findByStatusInAndNextAttemptAtLessThanEqualOrderByOutboxEventIdAsc(
            List<OutboxEventStatus> statuses,
            LocalDateTime now,
            Pageable pageable
    );
}
