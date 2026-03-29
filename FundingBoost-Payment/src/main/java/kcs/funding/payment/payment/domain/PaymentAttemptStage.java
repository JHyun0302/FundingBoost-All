package kcs.funding.payment.payment.domain;

public enum PaymentAttemptStage {
    INTENT_CREATED,
    POINT_RESERVED,
    PG_REQUESTED,
    PG_AUTHORIZED,
    CAPTURED
}

