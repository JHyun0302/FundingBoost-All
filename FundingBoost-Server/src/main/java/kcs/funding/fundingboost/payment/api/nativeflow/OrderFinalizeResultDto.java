package kcs.funding.fundingboost.payment.api.nativeflow;

public record OrderFinalizeResultDto(
        Long orderId,
        Long memberId,
        String paymentIntentKey,
        int totalAmount,
        int pointAmount,
        int pgAmount,
        int fundingSupportedAmount,
        Long sourceFundingId
) {
}
