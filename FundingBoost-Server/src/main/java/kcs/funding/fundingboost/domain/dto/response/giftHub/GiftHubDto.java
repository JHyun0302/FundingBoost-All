package kcs.funding.fundingboost.domain.dto.response.giftHub;

import kcs.funding.fundingboost.domain.entity.GiftHubItem;
import kcs.funding.fundingboost.domain.entity.Item;

public record GiftHubDto(Long itemId, String itemName, String itemImageUrl,
                         String optionName, int itemPrice, int quantity, Long giftHubItemId) {
    public static GiftHubDto createGiftHubDto(Item item, GiftHubItem giftHubItem) {
        String selectedOptionName = giftHubItem.getOptionName();
        if (selectedOptionName == null || selectedOptionName.isBlank()) {
            selectedOptionName = item.getOptionName();
        }
        return new GiftHubDto(item.getItemId(), item.getItemName(), item.getItemImageUrl(), selectedOptionName,
                item.getItemPrice(), giftHubItem.getQuantity(), giftHubItem.getGiftHubItemId());
    }
}
