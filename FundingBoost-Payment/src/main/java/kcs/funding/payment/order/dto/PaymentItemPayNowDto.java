package kcs.funding.payment.order.dto;

public record PaymentItemPayNowDto(
        Long itemId,
        int quantity,
        Long deliveryId,
        int usingPoint,
        String optionName
) {
}
