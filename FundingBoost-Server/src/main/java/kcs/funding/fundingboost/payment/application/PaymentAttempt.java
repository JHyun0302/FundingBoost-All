package kcs.funding.fundingboost.payment.application;

import kcs.funding.fundingboost.payment.domain.PaymentAttemptStage;

public record PaymentAttempt(
        Long paymentIntentId,
        int attemptNo,
        PaymentAttemptStage stage,
        String stageStatus,
        String provider,
        String providerTransactionId,
        String errorCode,
        String errorMessage,
        int requestedAmount
) {
    public static PaymentAttempt success(
            Long paymentIntentId,
            int attemptNo,
            PaymentAttemptStage stage,
            String provider,
            String providerTransactionId,
            int requestedAmount
    ) {
        return new PaymentAttempt(
                paymentIntentId,
                attemptNo,
                stage,
                "SUCCESS",
                provider,
                providerTransactionId,
                null,
                null,
                requestedAmount
        );
    }

    public static PaymentAttempt failure(
            Long paymentIntentId,
            int attemptNo,
            PaymentAttemptStage stage,
            String provider,
            String errorCode,
            String errorMessage,
            int requestedAmount
    ) {
        return new PaymentAttempt(
                paymentIntentId,
                attemptNo,
                stage,
                "FAILED",
                provider,
                null,
                errorCode,
                errorMessage,
                requestedAmount
        );
    }
}

