package kcs.funding.fundingboost.domain.dto.request.admin;

import kcs.funding.fundingboost.domain.entity.member.MemberRole;

public record AdminMemberRoleUpdateRequestDto(
        MemberRole role
) {
}
