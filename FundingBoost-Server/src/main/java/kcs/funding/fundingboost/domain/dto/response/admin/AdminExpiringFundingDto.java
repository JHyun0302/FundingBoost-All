package kcs.funding.fundingboost.domain.dto.response.admin;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AdminExpiringFundingDto(
        Long fundingId,
        String ownerName,
        String ownerProfileImgUrl,
        String tag,
        int totalPrice,
        int collectPrice,
        int progressPercent,
        int contributorCount,
        LocalDateTime deadline
) {
    public static AdminExpiringFundingDto from(
            Long fundingId,
            String ownerName,
            String ownerProfileImgUrl,
            String tag,
            int totalPrice,
            int collectPrice,
            int progressPercent,
            int contributorCount,
            LocalDateTime deadline
    ) {
        return AdminExpiringFundingDto.builder()
                .fundingId(fundingId)
                .ownerName(ownerName)
                .ownerProfileImgUrl(ownerProfileImgUrl)
                .tag(tag)
                .totalPrice(totalPrice)
                .collectPrice(collectPrice)
                .progressPercent(progressPercent)
                .contributorCount(contributorCount)
                .deadline(deadline)
                .build();
    }
}
