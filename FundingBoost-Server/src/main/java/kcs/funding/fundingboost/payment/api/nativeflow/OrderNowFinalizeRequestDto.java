package kcs.funding.fundingboost.payment.api.nativeflow;

import kcs.funding.fundingboost.domain.dto.request.pay.myPay.ItemPayNowDto;

public record OrderNowFinalizeRequestDto(
        ItemPayNowDto payload,
        String paymentIntentKey,
        int totalAmount,
        int pointAmount,
        int pgAmount,
        int fundingSupportedAmount,
        String pgProvider,
        String pgTransactionId
) {
}
