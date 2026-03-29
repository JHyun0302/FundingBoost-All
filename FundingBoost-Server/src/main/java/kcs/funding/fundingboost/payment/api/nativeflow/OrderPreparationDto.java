package kcs.funding.fundingboost.payment.api.nativeflow;

public record OrderPreparationDto(
        Long memberId,
        Long referenceId,
        String currency,
        int totalAmount,
        int pointAmount,
        int pgAmount,
        int fundingSupportedAmount
) {
}
