package kcs.funding.fundingboost.domain.dto.response.myPage.orderHistory;

import java.time.format.DateTimeFormatter;
import java.util.List;
import kcs.funding.fundingboost.domain.entity.OrderItem;
import lombok.Builder;

@Builder
public record OrderHistoryDetailDto(
        Long orderItemId,
        Long itemId,
        String itemName,
        String itemImageUrl,
        String optionName,
        int quantity,
        int totalPrice,
        String createdDate,
        String paymentLabel,
        int pointUsedAmount,
        int directPaidAmount,
        int fundingSupportedAmount,
        String customerName,
        String address,
        String postalCode,
        String phoneNumber,
        String deliveryMemo,
        List<OrderHistoryContributorDto> contributors
) {
    public static OrderHistoryDetailDto fromEntity(
            OrderItem orderItem,
            String paymentLabel,
            List<OrderHistoryContributorDto> contributors
    ) {
        return OrderHistoryDetailDto.builder()
                .orderItemId(orderItem.getId())
                .itemId(orderItem.getItem().getItemId())
                .itemName(orderItem.getItem().getItemName())
                .itemImageUrl(orderItem.getItem().getItemImageUrl())
                .optionName(resolveOptionName(orderItem))
                .quantity(orderItem.getQuantity())
                .totalPrice(orderItem.getItem().getItemPrice() * orderItem.getQuantity())
                .createdDate(orderItem.getOrder().getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .paymentLabel(paymentLabel)
                .pointUsedAmount(orderItem.getOrder().getPointUsedAmount())
                .directPaidAmount(orderItem.getOrder().getDirectPaidAmount())
                .fundingSupportedAmount(orderItem.getOrder().getFundingSupportedAmount())
                .customerName(orderItem.getOrder().getDelivery().getCustomerName())
                .address(orderItem.getOrder().getDelivery().getAddress())
                .postalCode(orderItem.getOrder().getDelivery().getPostalCode())
                .phoneNumber(orderItem.getOrder().getDelivery().getPhoneNumber())
                .deliveryMemo(orderItem.getOrder().getDelivery().getDeliveryMemo())
                .contributors(contributors)
                .build();
    }

    private static String resolveOptionName(OrderItem orderItem) {
        if (orderItem.getOptionName() != null && !orderItem.getOptionName().isBlank()) {
            return orderItem.getOptionName();
        }
        return orderItem.getItem().getOptionName();
    }
}
