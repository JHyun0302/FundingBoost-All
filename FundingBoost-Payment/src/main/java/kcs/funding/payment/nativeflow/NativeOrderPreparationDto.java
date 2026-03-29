package kcs.funding.payment.nativeflow;

public record NativeOrderPreparationDto(
        Long memberId,
        Long referenceId,
        String currency,
        int totalAmount,
        int pointAmount,
        int pgAmount,
        int fundingSupportedAmount
) {
}
