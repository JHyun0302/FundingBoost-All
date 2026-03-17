package kcs.funding.fundingboost.domain.dto.response.admin;

import lombok.Builder;

@Builder
public record AdminCategoryMetricDto(
        String category,
        long fundingCount,
        long orderQuantity,
        long wishlistCount,
        long score
) {
    public static AdminCategoryMetricDto from(
            String category,
            long fundingCount,
            long orderQuantity,
            long wishlistCount,
            long score
    ) {
        return AdminCategoryMetricDto.builder()
                .category(category)
                .fundingCount(fundingCount)
                .orderQuantity(orderQuantity)
                .wishlistCount(wishlistCount)
                .score(score)
                .build();
    }
}
