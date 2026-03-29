package kcs.funding.crawler.catalog.api;

import kcs.funding.crawler.entity.Item;

public record HomeRankingItemDto(
        Long itemId,
        String itemName,
        int price,
        String itemImageUrl,
        String brandName,
        long score,
        int rank
) {

    public static HomeRankingItemDto fromEntity(Item item, long score, int rank) {
        return new HomeRankingItemDto(
                item.getItemId(),
                item.getItemName(),
                item.getItemPrice(),
                item.getItemImageUrl(),
                item.getBrandName(),
                score,
                rank
        );
    }
}
