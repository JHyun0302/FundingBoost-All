package kcs.funding.fundingboost.domain.dto.response.shopping;

import kcs.funding.fundingboost.domain.entity.Item;
import kcs.funding.fundingboost.elasticsearch.index.ItemIndex;
import lombok.Builder;

@Builder
public record ShopDto(Long itemId, String itemName, String category, int price, String itemImageUrl, String brandName) {
    public static ShopDto fromEntity(Item item) {
        return ShopDto.builder()
                .itemId(item.getItemId())
                .itemName(item.getItemName())
                .category(item.getCategory())
                .price(item.getItemPrice())
                .itemImageUrl(item.getItemImageUrl())
                .brandName(item.getBrandName())
                .build();
    }

    public static ShopDto fromIndex(ItemIndex itemIndex) {
        return ShopDto.builder()
                .itemId(itemIndex.getItemId())
                .itemName(itemIndex.getItemName())
                .category(itemIndex.getCategory())
                .price(itemIndex.getItemPrice())
                .itemImageUrl(itemIndex.getItemImageUrl())
                .brandName(itemIndex.getBrandName())
                .build();
    }
}
