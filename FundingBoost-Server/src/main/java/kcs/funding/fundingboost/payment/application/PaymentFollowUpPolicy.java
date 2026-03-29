package kcs.funding.fundingboost.payment.application;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PaymentFollowUpPolicy {

    private final String followUpMode;

    public PaymentFollowUpPolicy(
            @Value("${app.payment.follow-up.mode:direct}") String followUpMode
    ) {
        this.followUpMode = followUpMode;
    }

    public boolean isConsumerMode() {
        return "consumer".equalsIgnoreCase(followUpMode);
    }
}
