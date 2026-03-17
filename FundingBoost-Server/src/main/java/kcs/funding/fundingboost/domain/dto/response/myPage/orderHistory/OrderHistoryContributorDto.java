package kcs.funding.fundingboost.domain.dto.response.myPage.orderHistory;

import kcs.funding.fundingboost.domain.entity.Contributor;
import lombok.Builder;

@Builder
public record OrderHistoryContributorDto(
        String nickname,
        String profileImgUrl,
        int fundedPrice
) {
    public static OrderHistoryContributorDto fromEntity(Contributor contributor) {
        return OrderHistoryContributorDto.builder()
                .nickname(contributor.getMember().getNickName())
                .profileImgUrl(contributor.getMember().getProfileImgUrl())
                .fundedPrice(contributor.getContributorPrice())
                .build();
    }
}
