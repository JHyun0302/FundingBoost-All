package kcs.funding.fundingboost.event.application;

import java.util.Map;
import java.util.Objects;

public record OutboxMessage(
        String topic,
        String eventKey,
        String eventType,
        String aggregateType,
        String aggregateId,
        Object payload,
        Map<String, String> headers
) {
    public OutboxMessage {
        Objects.requireNonNull(topic, "topic is required");
        Objects.requireNonNull(eventKey, "eventKey is required");
        Objects.requireNonNull(eventType, "eventType is required");
        Objects.requireNonNull(aggregateType, "aggregateType is required");
        Objects.requireNonNull(aggregateId, "aggregateId is required");
        Objects.requireNonNull(payload, "payload is required");
    }
}

