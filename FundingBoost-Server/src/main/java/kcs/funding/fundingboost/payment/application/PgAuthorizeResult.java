package kcs.funding.fundingboost.payment.application;

public record PgAuthorizeResult(
        boolean approved,
        String provider,
        String transactionId,
        String errorCode,
        String errorMessage
) {
    public static PgAuthorizeResult approved(String provider, String transactionId) {
        return new PgAuthorizeResult(true, provider, transactionId, null, null);
    }

    public static PgAuthorizeResult rejected(String provider, String errorCode, String errorMessage) {
        return new PgAuthorizeResult(false, provider, null, errorCode, errorMessage);
    }
}

