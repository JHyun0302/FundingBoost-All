package kcs.funding.fundingboost.domain.dto.response.member;

import kcs.funding.fundingboost.domain.entity.member.Member;
import kcs.funding.fundingboost.domain.entity.member.MemberGender;

public record MemberGenderStatusDto(
        MemberGender gender,
        boolean needsSetup
) {
    public static MemberGenderStatusDto fromEntity(Member member) {
        MemberGender gender = member.getGender() == null ? MemberGender.UNKNOWN : member.getGender();
        return new MemberGenderStatusDto(gender, gender == MemberGender.UNKNOWN);
    }
}
