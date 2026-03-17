package kcs.funding.fundingboost.domain.dto.response.admin;

import lombok.Builder;

@Builder
public record AdminMemberRoleUpdateDto(
        Long memberId,
        String email,
        String nickName,
        String role
) {
    public static AdminMemberRoleUpdateDto from(
            Long memberId,
            String email,
            String nickName,
            String role
    ) {
        return AdminMemberRoleUpdateDto.builder()
                .memberId(memberId)
                .email(email)
                .nickName(nickName)
                .role(role)
                .build();
    }
}
