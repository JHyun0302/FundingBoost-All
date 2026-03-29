package kcs.funding.fundingboost.payment.application.followup;

import com.fasterxml.jackson.databind.JsonNode;
import kcs.funding.fundingboost.event.application.OutboxEventService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FundingContributionPaymentCompletedFollowUpService {

    private final OutboxEventService outboxEventService;

    public void handle(JsonNode payload) {
        long fundingId = requiredLong(payload, "fundingId");
        long contributorId = requiredLong(payload, "contributorId");

        outboxEventService.enqueueFundingContributionCreated(
                fundingId,
                contributorId,
                requiredLong(payload, "contributorMemberId"),
                requiredLong(payload, "fundingOwnerMemberId"),
                requiredInt(payload, "fundingPrice"),
                requiredInt(payload, "usingPoint"),
                requiredInt(payload, "collectPrice"),
                requiredInt(payload, "totalPrice")
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
}
