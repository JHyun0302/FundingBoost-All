package kcs.funding.fundingboost.payment.application;

public record PgAuthorizeRequest(
        String intentKey,
        Long memberId,
        int amount,
        String currency
) {
}

