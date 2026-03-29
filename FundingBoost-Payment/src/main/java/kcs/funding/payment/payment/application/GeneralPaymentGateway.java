package kcs.funding.payment.payment.application;

public interface GeneralPaymentGateway {

    PgAuthorizeResult authorize(PgAuthorizeRequest request);
}

