package kcs.funding.payment.api;

public class PaymentProxyException extends RuntimeException {

    public PaymentProxyException(String message) {
        super(message);
    }

    public PaymentProxyException(String message, Throwable cause) {
        super(message, cause);
    }
}
