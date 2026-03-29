package kcs.funding.fundingboost.saga.application;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CommerceOrderPaidSagaHandler {

    public void handle(JsonNode payload) {
        long orderId = requiredLong(payload, "orderId");
        long memberId = requiredLong(payload, "memberId");

        log.info(
                "commerce order paid event captured for saga foundation: orderId={}, memberId={}, paymentIntentKey={}",
                orderId,
                memberId,
                textOrNull(payload, "paymentIntentKey")
        );
    }

    private long requiredLong(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        if (!node.canConvertToLong()) {
            throw new IllegalArgumentException("missing or invalid field: " + fieldName);
        }
        return node.asLong();
    }

    private String textOrNull(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText();
        return value == null || value.isBlank() ? null : value;
    }
}
