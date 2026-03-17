package kcs.funding.fundingboost.payment.application;

public interface GeneralPaymentGateway {

    PgAuthorizeResult authorize(PgAuthorizeRequest request);
}

