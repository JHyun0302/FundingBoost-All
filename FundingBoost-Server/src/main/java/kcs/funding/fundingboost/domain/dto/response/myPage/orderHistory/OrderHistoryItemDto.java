package kcs.funding.fundingboost.domain.dto.response.myPage.orderHistory;

import java.time.LocalDateTime;
import kcs.funding.fundingboost.domain.entity.OrderItem;
import lombok.Builder;

@Builder
public record OrderHistoryItemDto(
        Long orderItemId,
        String itemName,
        String itemImageUrl,
        String optionName,
        int quantity,
        int price,
        LocalDateTime createdDate
) {

    public static OrderHistoryItemDto fromEntity(OrderItem orderItem) {
        return OrderHistoryItemDto.builder()
                .orderItemId(orderItem.getId())
                .itemName(orderItem.getItem().getItemName())
                .itemImageUrl(orderItem.getItem().getItemImageUrl())
                .optionName(resolveOptionName(orderItem))
                .quantity(orderItem.getQuantity())
                .price(orderItem.getItem().getItemPrice() * orderItem.getQuantity()) // 총 가격을 반환
                .createdDate(orderItem.getOrder().getCreatedDate())
                .build();
    }

    private static String resolveOptionName(OrderItem orderItem) {
        if (orderItem.getOptionName() != null && !orderItem.getOptionName().isBlank()) {
            return orderItem.getOptionName();
        }
        return orderItem.getItem().getOptionName();
    }
}
