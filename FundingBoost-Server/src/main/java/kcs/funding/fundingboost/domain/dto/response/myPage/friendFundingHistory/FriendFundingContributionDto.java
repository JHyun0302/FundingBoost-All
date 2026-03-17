package kcs.funding.fundingboost.domain.dto.response.myPage.friendFundingHistory;

import java.time.format.DateTimeFormatter;
import kcs.funding.fundingboost.domain.entity.Contributor;
import kcs.funding.fundingboost.domain.entity.Funding;
import kcs.funding.fundingboost.domain.entity.FundingItem;
import lombok.Builder;

@Builder
public record FriendFundingContributionDto(Long friendMemberId,
                                           Long fundingId,
                                           String nickname,
                                           int price,
                                           String friendProfileImg,
                                           String tag,
                                           String createdDate,
                                           String itemImageUrl,
                                           String itemName,
                                           String optionName,
                                           int itemPrice) {
    public static FriendFundingContributionDto fromEntity(Contributor contributor, Funding friendFunding) {
        FundingItem fundingItem = friendFunding.getFundingItems().isEmpty()
                ? null
                : friendFunding.getFundingItems().get(0);

        return FriendFundingContributionDto.builder()
                .friendMemberId(friendFunding.getMember().getMemberId())
                .fundingId(friendFunding.getFundingId())
                .nickname(friendFunding.getMember().getNickName())
                .price(contributor.getContributorPrice())
                .friendProfileImg(friendFunding.getMember().getProfileImgUrl())
                .tag(friendFunding.getDisplayTag())
                .createdDate(friendFunding.getCreatedDate().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")))
                .itemImageUrl(fundingItem != null ? fundingItem.getItem().getItemImageUrl() : "")
                .itemName(fundingItem != null ? fundingItem.getItem().getItemName() : "")
                .optionName(fundingItem != null ? fundingItem.getItem().getOptionName() : "")
                .itemPrice(fundingItem != null ? fundingItem.getItem().getItemPrice() : 0)
                .build();
    }

}
