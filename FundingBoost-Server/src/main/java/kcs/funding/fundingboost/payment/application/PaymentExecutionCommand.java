package kcs.funding.fundingboost.payment.application;

import java.util.Objects;
import kcs.funding.fundingboost.payment.domain.PaymentIntentType;

public record PaymentExecutionCommand(
        Long memberId,
        PaymentIntentType intentType,
        Long referenceId,
        String idempotencyKey,
        int totalAmount,
        int pointAmount,
        int pgAmount,
        int fundingSupportedAmount,
        String currency
) {
    public PaymentExecutionCommand {
        Objects.requireNonNull(memberId, "memberId is required");
        Objects.requireNonNull(intentType, "intentType is required");
        Objects.requireNonNull(currency, "currency is required");

        if (totalAmount < 0 || pointAmount < 0 || pgAmount < 0 || fundingSupportedAmount < 0) {
            throw new IllegalArgumentException("payment amount must be non-negative");
        }
        if (pointAmount + pgAmount + fundingSupportedAmount != totalAmount) {
            throw new IllegalArgumentException("payment amount mismatch");
        }
    }
}
