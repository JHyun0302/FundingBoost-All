package kcs.funding.payment.payment.domain;

public enum PaymentIntentStatus {
    CREATED,
    POINT_RESERVED,
    PG_REQUESTED,
    PG_APPROVED,
    CAPTURED,
    FAILED,
    CANCELED;

    public boolean isTerminal() {
        return this == CAPTURED || this == FAILED || this == CANCELED;
    }
}

