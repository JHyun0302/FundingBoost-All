package kcs.funding.fundingboost.saga.application;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentFailedSagaHandler {

    public void handle(JsonNode payload) {
        String paymentIntentKey = requiredText(payload, "paymentIntentKey");

        log.warn(
                "payment failed event captured for saga compensation foundation: paymentIntentKey={}, paymentFlow={}, referenceId={}",
                paymentIntentKey,
                requiredText(payload, "paymentFlow"),
                longOrNull(payload, "referenceId")
        );
    }

    private String requiredText(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        String value = node.asText(null);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("missing or invalid field: " + fieldName);
        }
        return value;
    }

    private Long longOrNull(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (!node.canConvertToLong()) {
            throw new IllegalArgumentException("missing or invalid field: " + fieldName);
        }
        return node.asLong();
    }
}
