package kcs.funding.fundingboost.payment.application;

import kcs.funding.fundingboost.payment.domain.PaymentIntentStatus;

public record PaymentExecutionResult(
        String intentKey,
        PaymentIntentStatus status,
        String pgProvider,
        String pgTransactionId
) {
}

