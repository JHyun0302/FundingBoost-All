package kcs.funding.fundingboost.domain.dto.request.login;

import kcs.funding.fundingboost.domain.entity.member.MemberGender;

public record SignupDto(
        String nickName,
        String password,
        String email,
        MemberGender gender
) {
}
