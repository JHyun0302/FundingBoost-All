package kcs.funding.fundingboost.payment.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import lombok.Getter;

@Getter
public class PaymentIntent {

    private Long paymentIntentId;
    private final String intentKey;
    private final Long memberId;
    private final PaymentIntentType intentType;
    private final Long referenceId;
    private Long orderId;
    private final String currency;
    private final int totalAmount;
    private final int pointAmount;
    private final int pgAmount;
    private final int fundingSupportedAmount;
    private PaymentIntentStatus status;
    private String pgProvider;
    private String pgTransactionId;
    private String failureCode;
    private String failureMessage;
    private final LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private PaymentIntent(
            Long paymentIntentId,
            String intentKey,
            Long memberId,
            PaymentIntentType intentType,
            Long referenceId,
            Long orderId,
            String currency,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount,
            PaymentIntentStatus status,
            String pgProvider,
            String pgTransactionId,
            String failureCode,
            String failureMessage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        this.paymentIntentId = paymentIntentId;
        this.intentKey = intentKey;
        this.memberId = memberId;
        this.intentType = intentType;
        this.referenceId = referenceId;
        this.orderId = orderId;
        this.currency = currency;
        this.totalAmount = totalAmount;
        this.pointAmount = pointAmount;
        this.pgAmount = pgAmount;
        this.fundingSupportedAmount = fundingSupportedAmount;
        this.status = status;
        this.pgProvider = pgProvider;
        this.pgTransactionId = pgTransactionId;
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static PaymentIntent create(
            String intentKey,
            Long memberId,
            PaymentIntentType intentType,
            Long referenceId,
            String currency,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount
    ) {
        Objects.requireNonNull(intentKey, "intentKey is required");
        Objects.requireNonNull(memberId, "memberId is required");
        Objects.requireNonNull(intentType, "intentType is required");
        Objects.requireNonNull(currency, "currency is required");

        if (totalAmount < 0 || pointAmount < 0 || pgAmount < 0 || fundingSupportedAmount < 0) {
            throw new IllegalArgumentException("payment amount must be non-negative");
        }
        if (pointAmount + pgAmount + fundingSupportedAmount != totalAmount) {
            throw new IllegalArgumentException("payment amount mismatch");
        }

        LocalDateTime now = LocalDateTime.now();
        return new PaymentIntent(
                null,
                intentKey,
                memberId,
                intentType,
                referenceId,
                null,
                currency,
                totalAmount,
                pointAmount,
                pgAmount,
                fundingSupportedAmount,
                PaymentIntentStatus.CREATED,
                null,
                null,
                null,
                null,
                now,
                now
        );
    }

    public static PaymentIntent rehydrate(
            Long paymentIntentId,
            String intentKey,
            Long memberId,
            PaymentIntentType intentType,
            Long referenceId,
            Long orderId,
            String currency,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount,
            PaymentIntentStatus status,
            String pgProvider,
            String pgTransactionId,
            String failureCode,
            String failureMessage,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        return new PaymentIntent(
                paymentIntentId,
                intentKey,
                memberId,
                intentType,
                referenceId,
                orderId,
                currency,
                totalAmount,
                pointAmount,
                pgAmount,
                fundingSupportedAmount,
                status,
                pgProvider,
                pgTransactionId,
                failureCode,
                failureMessage,
                createdAt,
                updatedAt
        );
    }

    public void assignId(Long paymentIntentId) {
        this.paymentIntentId = paymentIntentId;
    }

    public void transitionTo(PaymentIntentStatus nextStatus) {
        PaymentIntentStateMachine.validateTransition(this.status, nextStatus);
        this.status = nextStatus;
        this.updatedAt = LocalDateTime.now();
    }

    public void approvePg(String pgProvider, String pgTransactionId) {
        transitionTo(PaymentIntentStatus.PG_APPROVED);
        this.pgProvider = pgProvider;
        this.pgTransactionId = pgTransactionId;
        this.failureCode = null;
        this.failureMessage = null;
    }

    public void fail(String failureCode, String failureMessage) {
        transitionTo(PaymentIntentStatus.FAILED);
        this.failureCode = failureCode;
        this.failureMessage = failureMessage;
    }

    public void attachOrderId(Long orderId) {
        this.orderId = orderId;
        this.updatedAt = LocalDateTime.now();
    }
}

