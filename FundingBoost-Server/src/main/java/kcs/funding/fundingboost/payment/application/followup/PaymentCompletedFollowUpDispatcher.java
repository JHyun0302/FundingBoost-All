package kcs.funding.fundingboost.payment.application.followup;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentCompletedFollowUpDispatcher {

    private final FundingContributionPaymentCompletedFollowUpService fundingContributionFollowUpService;
    private final OrderPaymentCompletedFollowUpService orderFollowUpService;

    public void dispatch(JsonNode payload) {
        String paymentFlow = payload.path("paymentFlow").asText("");
        if ("FRIEND_FUNDING".equals(paymentFlow)) {
            fundingContributionFollowUpService.handle(payload);
            return;
        }
        orderFollowUpService.handle(payload, paymentFlow);
    }
}
