package kcs.funding.fundingboost.domain.dto.response.admin;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AdminRecentBarcodeTokenDto(
        String token,
        String status,
        String issuedBy,
        Long fundingId,
        String fundingOwner,
        int fundingPrice,
        int usingPoint,
        LocalDateTime issuedAt,
        LocalDateTime expiresAt,
        LocalDateTime usedAt
) {
    public static AdminRecentBarcodeTokenDto from(
            String token,
            String status,
            String issuedBy,
            Long fundingId,
            String fundingOwner,
            int fundingPrice,
            int usingPoint,
            LocalDateTime issuedAt,
            LocalDateTime expiresAt,
            LocalDateTime usedAt
    ) {
        return AdminRecentBarcodeTokenDto.builder()
                .token(token)
                .status(status)
                .issuedBy(issuedBy)
                .fundingId(fundingId)
                .fundingOwner(fundingOwner)
                .fundingPrice(fundingPrice)
                .usingPoint(usingPoint)
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .usedAt(usedAt)
                .build();
    }
}
