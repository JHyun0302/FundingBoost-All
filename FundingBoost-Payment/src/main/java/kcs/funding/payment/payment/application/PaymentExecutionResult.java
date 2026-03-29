package kcs.funding.payment.payment.application;

import kcs.funding.payment.payment.domain.PaymentIntentStatus;

public record PaymentExecutionResult(
        String intentKey,
        PaymentIntentStatus status,
        String pgProvider,
        String pgTransactionId
) {
}

