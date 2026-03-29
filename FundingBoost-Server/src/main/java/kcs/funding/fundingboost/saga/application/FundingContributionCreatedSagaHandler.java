package kcs.funding.fundingboost.saga.application;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class FundingContributionCreatedSagaHandler {

    public void handle(JsonNode payload) {
        long fundingId = requiredLong(payload, "fundingId");
        long contributorId = requiredLong(payload, "contributorId");

        log.info(
                "funding contribution event captured for saga foundation: fundingId={}, contributorId={}, contributorMemberId={}",
                fundingId,
                contributorId,
                requiredLong(payload, "contributorMemberId")
        );
    }

    private long requiredLong(JsonNode payload, String fieldName) {
        JsonNode node = payload.path(fieldName);
        if (!node.canConvertToLong()) {
            throw new IllegalArgumentException("missing or invalid field: " + fieldName);
        }
        return node.asLong();
    }
}
