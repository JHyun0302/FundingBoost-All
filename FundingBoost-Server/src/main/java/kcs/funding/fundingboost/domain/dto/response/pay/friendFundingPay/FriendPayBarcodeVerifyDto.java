package kcs.funding.fundingboost.domain.dto.response.pay.friendFundingPay;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record FriendPayBarcodeVerifyDto(
        String token,
        String status,
        Long fundingId,
        String friendName,
        int usingPoint,
        int fundingPrice,
        LocalDateTime expiresAt,
        boolean expired,
        boolean used
) {
}

