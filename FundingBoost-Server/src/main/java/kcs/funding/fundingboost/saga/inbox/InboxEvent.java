package kcs.funding.fundingboost.saga.inbox;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import kcs.funding.fundingboost.domain.entity.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "inbox_event",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_inbox_event_consumer_topic_key",
                        columnNames = {"consumer_name", "topic", "event_key"}
                )
        }
)
public class InboxEvent extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "inbox_event_id")
    private Long inboxEventId;

    @Column(name = "consumer_name", nullable = false, length = 80)
    private String consumerName;

    @Column(name = "topic", nullable = false, length = 120)
    private String topic;

    @Column(name = "event_key", nullable = false, length = 160)
    private String eventKey;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private InboxEventStatus status;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    public static InboxEvent received(
            String consumerName,
            String topic,
            String eventKey,
            String eventType,
            String payload
    ) {
        InboxEvent inboxEvent = new InboxEvent();
        inboxEvent.consumerName = consumerName;
        inboxEvent.topic = topic;
        inboxEvent.eventKey = eventKey;
        inboxEvent.eventType = eventType;
        inboxEvent.payload = payload;
        inboxEvent.status = InboxEventStatus.RECEIVED;
        return inboxEvent;
    }

    public boolean isCompleted() {
        return status == InboxEventStatus.COMPLETED;
    }

    public void prepareForRetry(String payload) {
        this.payload = payload;
        this.status = InboxEventStatus.RECEIVED;
        this.lastError = null;
        this.processedAt = null;
    }

    public void markCompleted() {
        this.status = InboxEventStatus.COMPLETED;
        this.lastError = null;
        this.processedAt = LocalDateTime.now();
    }

    public void markFailed(String errorMessage) {
        this.status = InboxEventStatus.FAILED;
        this.lastError = truncate(errorMessage, 500);
        this.processedAt = LocalDateTime.now();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
