package kcs.funding.payment.order.dto;

import java.util.List;

public record PaymentMyPayDto(
        List<PaymentItemPayDto> itemPayDtoList,
        Long deliveryId,
        int usingPoint
) {
}
