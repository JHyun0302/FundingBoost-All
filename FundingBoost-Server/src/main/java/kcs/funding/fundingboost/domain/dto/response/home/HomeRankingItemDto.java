package kcs.funding.fundingboost.domain.dto.response.home;

import kcs.funding.fundingboost.elasticsearch.index.ItemIndex;
import lombok.Builder;

@Builder
public record HomeRankingItemDto(
        Long itemId,
        String itemName,
        int price,
        String itemImageUrl,
        String brandName,
        long score,
        int rank
) {
    public static HomeRankingItemDto fromIndex(ItemIndex itemIndex, long score, int rank) {
        return HomeRankingItemDto.builder()
                .itemId(itemIndex.getItemId())
                .itemName(itemIndex.getItemName())
                .price(itemIndex.getItemPrice())
                .itemImageUrl(itemIndex.getItemImageUrl())
                .brandName(itemIndex.getBrandName())
                .score(score)
                .rank(rank)
                .build();
    }
}
