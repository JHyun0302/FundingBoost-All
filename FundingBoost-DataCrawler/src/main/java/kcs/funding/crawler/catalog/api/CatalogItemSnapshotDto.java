package kcs.funding.crawler.catalog.api;

import kcs.funding.crawler.entity.Item;

public record CatalogItemSnapshotDto(
        Long itemId,
        String itemName,
        int itemPrice,
        String itemImageUrl,
        String brandName,
        String category,
        String optionName
) {

    public static CatalogItemSnapshotDto fromEntity(Item item) {
        return new CatalogItemSnapshotDto(
                item.getItemId(),
                item.getItemName(),
                item.getItemPrice(),
                item.getItemImageUrl(),
                item.getBrandName(),
                item.getCategory(),
                item.getOptionName()
        );
    }
}
