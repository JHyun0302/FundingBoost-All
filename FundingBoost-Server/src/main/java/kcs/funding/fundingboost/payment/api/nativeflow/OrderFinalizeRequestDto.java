package kcs.funding.fundingboost.payment.api.nativeflow;

import kcs.funding.fundingboost.domain.dto.request.pay.myPay.MyPayDto;

public record OrderFinalizeRequestDto(
        MyPayDto payload,
        String paymentIntentKey,
        int totalAmount,
        int pointAmount,
        int pgAmount,
        int fundingSupportedAmount,
        String pgProvider,
        String pgTransactionId
) {
}
