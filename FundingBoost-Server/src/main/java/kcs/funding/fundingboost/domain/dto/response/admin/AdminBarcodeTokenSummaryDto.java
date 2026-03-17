package kcs.funding.fundingboost.domain.dto.response.admin;

import lombok.Builder;

@Builder
public record AdminBarcodeTokenSummaryDto(
        long pendingCount,
        long usedCount,
        long expiredCount
) {
    public static AdminBarcodeTokenSummaryDto from(long pendingCount, long usedCount, long expiredCount) {
        return AdminBarcodeTokenSummaryDto.builder()
                .pendingCount(pendingCount)
                .usedCount(usedCount)
                .expiredCount(expiredCount)
                .build();
    }
}
