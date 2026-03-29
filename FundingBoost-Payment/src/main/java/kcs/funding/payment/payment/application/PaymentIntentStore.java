package kcs.funding.payment.payment.application;

import java.util.Optional;
import kcs.funding.payment.payment.domain.PaymentIntent;

public interface PaymentIntentStore {
    void save(PaymentIntent paymentIntent);

    void update(PaymentIntent paymentIntent);

    Optional<PaymentIntent> findByIntentKey(String intentKey);

    void saveAttempt(PaymentAttempt paymentAttempt);
}

