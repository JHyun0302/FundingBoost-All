package kcs.funding.fundingboost.domain.dto.response.admin;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AdminMemberSummaryDto(
        Long memberId,
        String nickName,
        String email,
        String role,
        String gender,
        int point,
        LocalDateTime joinedAt
) {
    public static AdminMemberSummaryDto from(
            Long memberId,
            String nickName,
            String email,
            String role,
            String gender,
            int point,
            LocalDateTime joinedAt
    ) {
        return AdminMemberSummaryDto.builder()
                .memberId(memberId)
                .nickName(nickName)
                .email(email)
                .role(role)
                .gender(gender)
                .point(point)
                .joinedAt(joinedAt)
                .build();
    }
}
