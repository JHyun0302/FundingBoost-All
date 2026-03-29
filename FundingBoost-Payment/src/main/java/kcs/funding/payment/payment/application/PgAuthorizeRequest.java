package kcs.funding.payment.payment.application;

public record PgAuthorizeRequest(
        String intentKey,
        Long memberId,
        int amount,
        String currency
) {
}

