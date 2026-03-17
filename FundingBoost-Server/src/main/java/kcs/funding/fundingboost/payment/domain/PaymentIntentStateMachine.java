package kcs.funding.fundingboost.payment.domain;

import java.util.EnumSet;
import java.util.Map;

public final class PaymentIntentStateMachine {

    private static final Map<PaymentIntentStatus, EnumSet<PaymentIntentStatus>> ALLOWED_TRANSITIONS = Map.of(
            PaymentIntentStatus.CREATED, EnumSet.of(
                    PaymentIntentStatus.POINT_RESERVED,
                    PaymentIntentStatus.PG_REQUESTED,
                    PaymentIntentStatus.CAPTURED,
                    PaymentIntentStatus.FAILED,
                    PaymentIntentStatus.CANCELED
            ),
            PaymentIntentStatus.POINT_RESERVED, EnumSet.of(
                    PaymentIntentStatus.PG_REQUESTED,
                    PaymentIntentStatus.CAPTURED,
                    PaymentIntentStatus.FAILED,
                    PaymentIntentStatus.CANCELED
            ),
            PaymentIntentStatus.PG_REQUESTED, EnumSet.of(
                    PaymentIntentStatus.PG_APPROVED,
                    PaymentIntentStatus.FAILED,
                    PaymentIntentStatus.CANCELED
            ),
            PaymentIntentStatus.PG_APPROVED, EnumSet.of(
                    PaymentIntentStatus.CAPTURED,
                    PaymentIntentStatus.FAILED,
                    PaymentIntentStatus.CANCELED
            ),
            PaymentIntentStatus.CAPTURED, EnumSet.noneOf(PaymentIntentStatus.class),
            PaymentIntentStatus.FAILED, EnumSet.noneOf(PaymentIntentStatus.class),
            PaymentIntentStatus.CANCELED, EnumSet.noneOf(PaymentIntentStatus.class)
    );

    private PaymentIntentStateMachine() {
    }

    public static void validateTransition(PaymentIntentStatus current, PaymentIntentStatus next) {
        EnumSet<PaymentIntentStatus> allowed = ALLOWED_TRANSITIONS.get(current);
        if (allowed == null || !allowed.contains(next)) {
            throw new IllegalStateException(
                    "Invalid payment state transition: " + current + " -> " + next
            );
        }
    }
}

