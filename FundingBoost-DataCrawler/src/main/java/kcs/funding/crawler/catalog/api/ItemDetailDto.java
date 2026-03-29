package kcs.funding.crawler.catalog.api;

import java.util.List;
import kcs.funding.crawler.catalog.domain.ItemOptionParser;
import kcs.funding.crawler.entity.Item;

public record ItemDetailDto(
        Long itemId,
        String itemThumbnailImageUrl,
        String itemName,
        String brandName,
        String category,
        int itemPrice,
        boolean bookmark,
        String optionName,
        List<String> options
) {

    public static ItemDetailDto fromEntity(Item item, boolean bookmark) {
        return new ItemDetailDto(
                item.getItemId(),
                item.getItemImageUrl(),
                item.getItemName(),
                item.getBrandName(),
                item.getCategory(),
                item.getItemPrice(),
                bookmark,
                item.getOptionName(),
                ItemOptionParser.parseOptions(item.getOptionName())
        );
    }
}
