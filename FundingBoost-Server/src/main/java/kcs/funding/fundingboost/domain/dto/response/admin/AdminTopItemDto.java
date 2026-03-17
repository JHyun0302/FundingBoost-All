package kcs.funding.fundingboost.domain.dto.response.admin;

import lombok.Builder;

@Builder
public record AdminTopItemDto(
        Long itemId,
        String itemName,
        String brandName,
        String category,
        String itemImageUrl,
        int itemPrice,
        long fundingCount,
        long orderQuantity,
        long wishlistCount,
        long score
) {
    public static AdminTopItemDto from(
            Long itemId,
            String itemName,
            String brandName,
            String category,
            String itemImageUrl,
            int itemPrice,
            long fundingCount,
            long orderQuantity,
            long wishlistCount,
            long score
    ) {
        return AdminTopItemDto.builder()
                .itemId(itemId)
                .itemName(itemName)
                .brandName(brandName)
                .category(category)
                .itemImageUrl(itemImageUrl)
                .itemPrice(itemPrice)
                .fundingCount(fundingCount)
                .orderQuantity(orderQuantity)
                .wishlistCount(wishlistCount)
                .score(score)
                .build();
    }
}
