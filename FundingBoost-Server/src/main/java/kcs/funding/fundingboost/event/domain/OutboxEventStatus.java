package kcs.funding.fundingboost.event.domain;

public enum OutboxEventStatus {
    PENDING,
    RETRY,
    PUBLISHED,
    DEAD
}

