package kcs.funding.fundingboost.payment.application.followup;

import com.fasterxml.jackson.databind.JsonNode;
import kcs.funding.fundingboost.event.application.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class OrderPaymentCompletedFollowUpService {

    private final OutboxEventService outboxEventService;

    public void handle(JsonNode payload, String paymentFlow) {
        outboxEventService.enqueueOrderPaid(
                requiredLong(payload, "orderId"),
                requiredLong(payload, "memberId"),
                paymentFlow,
                textOrNull(payload, "paymentIntentKey"),
                requiredInt(payload, "totalAmount"),
                requiredInt(payload, "pointAmount"),
                requiredInt(payload, "pgAmount"),
                requiredInt(payload, "fundingSupportedAmount"),
                longOrNull(payload, "sourceFundingId")
        );
    }

    private long requiredLong(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        if (!node.canConvertToLong()) {
            throw new IllegalArgumentException("missing or invalid field: " + fieldName);
        }
        return node.asLong();
    }

    private int requiredInt(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        if (!node.canConvertToInt()) {
            throw new IllegalArgumentException("missing or invalid field: " + fieldName);
        }
        return node.asInt();
    }

    private String textOrNull(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        if (node.isMissingNode() || node.isNull() || node.asText().isBlank()) {
            return null;
        }
        return node.asText();
    }

    private Long longOrNull(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        return node.asLong();
    }
}
