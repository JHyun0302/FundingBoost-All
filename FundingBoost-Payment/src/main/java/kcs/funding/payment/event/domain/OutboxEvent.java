package kcs.funding.payment.event.domain;

import static jakarta.persistence.EnumType.STRING;
import static jakarta.persistence.GenerationType.IDENTITY;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import kcs.funding.payment.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "outbox_event",
        indexes = {
                @Index(name = "idx_outbox_status_next_attempt", columnList = "status,next_attempt_at"),
                @Index(name = "idx_outbox_created_date", columnList = "created_date")
        }
)
public class OutboxEvent extends BaseTimeEntity {

    private static final int MAX_ERROR_LENGTH = 500;
    private static final long MAX_BACKOFF_SECONDS = 3600L;

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "outbox_event_id")
    private Long outboxEventId;

    @Column(name = "topic", nullable = false, length = 150)
    private String topic;

    @Column(name = "event_key", nullable = false, length = 120)
    private String eventKey;

    @Column(name = "event_type", nullable = false, length = 120)
    private String eventType;

    @Column(name = "aggregate_type", nullable = false, length = 80)
    private String aggregateType;

    @Column(name = "aggregate_id", nullable = false, length = 120)
    private String aggregateId;

    @Lob
    @Column(name = "payload", nullable = false, columnDefinition = "TEXT")
    private String payload;

    @Lob
    @Column(name = "headers", columnDefinition = "TEXT")
    private String headers;

    @Enumerated(STRING)
    @Column(name = "status", nullable = false, length = 20)
    private OutboxEventStatus status;

    @Column(name = "retry_count", nullable = false)
    private int retryCount;

    @Column(name = "next_attempt_at", nullable = false)
    private LocalDateTime nextAttemptAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "last_error", length = MAX_ERROR_LENGTH)
    private String lastError;

    public static OutboxEvent create(
            String topic,
            String eventKey,
            String eventType,
            String aggregateType,
            String aggregateId,
            String payload,
            String headers,
            LocalDateTime now
    ) {
        OutboxEvent event = new OutboxEvent();
        event.topic = topic;
        event.eventKey = eventKey;
        event.eventType = eventType;
        event.aggregateType = aggregateType;
        event.aggregateId = aggregateId;
        event.payload = payload;
        event.headers = headers;
        event.status = OutboxEventStatus.PENDING;
        event.retryCount = 0;
        event.nextAttemptAt = now;
        event.publishedAt = null;
        event.lastError = null;
        return event;
    }

    public boolean isReadyAt(LocalDateTime now) {
        if (status != OutboxEventStatus.PENDING && status != OutboxEventStatus.RETRY) {
            return false;
        }
        return !nextAttemptAt.isAfter(now);
    }

    public void markPublished(LocalDateTime now) {
        this.status = OutboxEventStatus.PUBLISHED;
        this.publishedAt = now;
        this.lastError = null;
    }

    public void markRetry(int maxRetry, int baseRetrySeconds, LocalDateTime now, String errorMessage) {
        this.retryCount += 1;
        this.lastError = trimError(errorMessage);
        this.publishedAt = null;

        if (retryCount > maxRetry) {
            this.status = OutboxEventStatus.DEAD;
            this.nextAttemptAt = now;
            return;
        }

        this.status = OutboxEventStatus.RETRY;
        this.nextAttemptAt = now.plusSeconds(calculateBackoffSeconds(baseRetrySeconds));
    }

    private long calculateBackoffSeconds(int baseRetrySeconds) {
        long backoff = Math.max(baseRetrySeconds, 1);
        int exponent = Math.max(retryCount - 1, 0);
        for (int i = 0; i < exponent; i++) {
            backoff *= 2;
            if (backoff >= MAX_BACKOFF_SECONDS) {
                return MAX_BACKOFF_SECONDS;
            }
        }
        return Math.min(backoff, MAX_BACKOFF_SECONDS);
    }

    private String trimError(String errorMessage) {
        if (errorMessage == null || errorMessage.isBlank()) {
            return "unknown error";
        }
        if (errorMessage.length() <= MAX_ERROR_LENGTH) {
            return errorMessage;
        }
        return errorMessage.substring(0, MAX_ERROR_LENGTH);
    }
}

