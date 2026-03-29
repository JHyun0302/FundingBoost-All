package kcs.funding.payment.nativeflow;

public record NativeOrderFinalizeResultDto(
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
