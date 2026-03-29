package kcs.funding.payment.event.domain;

public enum OutboxEventStatus {
    PENDING,
    RETRY,
    PUBLISHED,
    DEAD
}

