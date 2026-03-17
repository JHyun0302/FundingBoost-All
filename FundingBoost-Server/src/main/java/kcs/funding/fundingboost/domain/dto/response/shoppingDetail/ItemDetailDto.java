package kcs.funding.fundingboost.domain.dto.response.shoppingDetail;

import java.util.List;
import kcs.funding.fundingboost.domain.entity.Item;
import kcs.funding.fundingboost.domain.utils.ItemOptionParser;
import kcs.funding.fundingboost.elasticsearch.index.ItemIndex;
import lombok.Builder;

@Builder
public record ItemDetailDto(
        String itemThumbnailImageUrl,
        String itemName,
        String brandName,
        String category,
        int itemPrice,
        boolean bookmark,
        List<String> options
) {
    public static ItemDetailDto fromEntity(Item item, boolean bookmark) {
        return ItemDetailDto.builder()
                .itemThumbnailImageUrl(item.getItemImageUrl())
                .itemName(item.getItemName())
                .brandName(item.getBrandName())
                .category(item.getCategory())
                .itemPrice(item.getItemPrice())
                .bookmark(bookmark)
                .options(ItemOptionParser.parseOptions(item.getOptionName()))
                .build();
    }

    public static ItemDetailDto fromIndex(ItemIndex itemIndex, boolean bookmark) {
        return ItemDetailDto.builder()
                .itemThumbnailImageUrl(itemIndex.getItemImageUrl())
                .itemName(itemIndex.getItemName())
                .brandName(itemIndex.getBrandName())
                .category(itemIndex.getCategory())
                .itemPrice(itemIndex.getItemPrice())
                .bookmark(bookmark)
                .options(ItemOptionParser.parseOptions(itemIndex.getOptionName()))
                .build();
    }
}
