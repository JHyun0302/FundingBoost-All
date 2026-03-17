package kcs.funding.fundingboost.domain.dto.request.member;

import kcs.funding.fundingboost.domain.entity.member.MemberGender;

public record UpdateMemberGenderDto(
        MemberGender gender
) {
}
