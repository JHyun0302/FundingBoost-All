package kcs.funding.payment.friend.dto;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record FriendPayBarcodeIssueDto(
        String token,
        String barcodeValue,
        String verifyUrl,
        LocalDateTime expiresAt,
        int usingPoint,
        int fundingPrice
) {
}
