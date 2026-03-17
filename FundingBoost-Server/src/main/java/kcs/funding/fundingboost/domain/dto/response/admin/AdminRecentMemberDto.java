package kcs.funding.fundingboost.domain.dto.response.admin;

import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record AdminRecentMemberDto(
        Long memberId,
        String nickName,
        String email,
        String role,
        String gender,
        LocalDateTime joinedAt
) {
    public static AdminRecentMemberDto from(
            Long memberId,
            String nickName,
            String email,
            String role,
            String gender,
            LocalDateTime joinedAt
    ) {
        return AdminRecentMemberDto.builder()
                .memberId(memberId)
                .nickName(nickName)
                .email(email)
                .role(role)
                .gender(gender)
                .joinedAt(joinedAt)
                .build();
    }
}
