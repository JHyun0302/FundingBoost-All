package kcs.funding.fundingboost.domain.dto.response.admin;

import lombok.Builder;

@Builder
public record AdminDashboardKpiDto(
        long totalMembers,
        long adminMembers,
        long totalFundings,
        long activeFundings,
        long totalOrders,
        long todayOrders,
        long totalRevenue,
        long todayRevenue
) {
    public static AdminDashboardKpiDto from(
            long totalMembers,
            long adminMembers,
            long totalFundings,
            long activeFundings,
            long totalOrders,
            long todayOrders,
            long totalRevenue,
            long todayRevenue
    ) {
        return AdminDashboardKpiDto.builder()
                .totalMembers(totalMembers)
                .adminMembers(adminMembers)
                .totalFundings(totalFundings)
                .activeFundings(activeFundings)
                .totalOrders(totalOrders)
                .todayOrders(todayOrders)
                .totalRevenue(totalRevenue)
                .todayRevenue(todayRevenue)
                .build();
    }
}
