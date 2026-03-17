package kcs.funding.fundingboost.domain.dto.response.admin;

import java.util.List;
import lombok.Builder;

@Builder
public record AdminDashboardDto(
        AdminDashboardKpiDto kpi,
        List<AdminCategoryMetricDto> categoryMetrics,
        List<AdminTopItemDto> topItems,
        List<AdminExpiringFundingDto> expiringFundings,
        List<AdminRecentMemberDto> recentMembers,
        AdminBarcodeTokenSummaryDto barcodeTokenSummary,
        List<AdminRecentBarcodeTokenDto> recentBarcodeTokens
) {
    public static AdminDashboardDto from(
            AdminDashboardKpiDto kpi,
            List<AdminCategoryMetricDto> categoryMetrics,
            List<AdminTopItemDto> topItems,
            List<AdminExpiringFundingDto> expiringFundings,
            List<AdminRecentMemberDto> recentMembers,
            AdminBarcodeTokenSummaryDto barcodeTokenSummary,
            List<AdminRecentBarcodeTokenDto> recentBarcodeTokens
    ) {
        return AdminDashboardDto.builder()
                .kpi(kpi)
                .categoryMetrics(categoryMetrics)
                .topItems(topItems)
                .expiringFundings(expiringFundings)
                .recentMembers(recentMembers)
                .barcodeTokenSummary(barcodeTokenSummary)
                .recentBarcodeTokens(recentBarcodeTokens)
                .build();
    }
}
