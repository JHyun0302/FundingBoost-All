package kcs.funding.fundingboost.payment.application;

import java.util.Optional;
import kcs.funding.fundingboost.payment.domain.PaymentIntent;

public interface PaymentIntentStore {
    void save(PaymentIntent paymentIntent);

    void update(PaymentIntent paymentIntent);

    Optional<PaymentIntent> findByIntentKey(String intentKey);

    void saveAttempt(PaymentAttempt paymentAttempt);
}

