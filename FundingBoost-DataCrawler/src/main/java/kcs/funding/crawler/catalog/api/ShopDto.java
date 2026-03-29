package kcs.funding.crawler.catalog.api;

import kcs.funding.crawler.entity.Item;

public record ShopDto(
        Long itemId,
        String itemName,
        String category,
        int price,
        String itemImageUrl,
        String brandName
) {

    public static ShopDto fromEntity(Item item) {
        return new ShopDto(
                item.getItemId(),
                item.getItemName(),
                item.getCategory(),
                item.getItemPrice(),
                item.getItemImageUrl(),
                item.getBrandName()
        );
    }
}
