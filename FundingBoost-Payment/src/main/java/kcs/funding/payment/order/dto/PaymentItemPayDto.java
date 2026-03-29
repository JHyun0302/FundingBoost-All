package kcs.funding.payment.order.dto;

public record PaymentItemPayDto(
        Long itemId,
        Long giftHubId,
        int quantity,
        String optionName
) {
}
