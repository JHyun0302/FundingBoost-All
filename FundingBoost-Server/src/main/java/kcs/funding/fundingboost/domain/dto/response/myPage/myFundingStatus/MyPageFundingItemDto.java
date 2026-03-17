package kcs.funding.fundingboost.domain.dto.response.myPage.myFundingStatus;

import kcs.funding.fundingboost.domain.entity.Funding;
import kcs.funding.fundingboost.domain.entity.FundingItem;
import lombok.Builder;

@Builder
public record MyPageFundingItemDto(
        Long fundingId,
        Long fundingItemId,
        Long itemId,
        String itemName,
        int itemPrice,
        String itemImageUrl,
        String optionName,
        int itemPercent,
        boolean finishedStatus,
        boolean itemStatus
) {
    public static MyPageFundingItemDto fromEntity(
            Funding funding,
            FundingItem fundingItem,
            int itemPercent
    ) {
        return MyPageFundingItemDto.builder()
                .fundingId(funding.getFundingId())
                .fundingItemId(fundingItem.getFundingItemId())
                .itemId(fundingItem.getItem().getItemId())
                .itemName(fundingItem.getItem().getItemName())
                .itemPrice(fundingItem.getItem().getItemPrice())
                .itemImageUrl(fundingItem.getItem().getItemImageUrl())
                .optionName(fundingItem.getItem().getOptionName())
                .itemPercent(itemPercent)
                .finishedStatus(fundingItem.isFinishedStatus())
                .itemStatus(fundingItem.isItemStatus())
                .build();
    }
}
