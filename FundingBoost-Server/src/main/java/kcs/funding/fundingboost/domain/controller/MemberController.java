package kcs.funding.fundingboost.domain.controller;

import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.dto.request.member.UpdateMemberGenderDto;
import kcs.funding.fundingboost.domain.dto.request.myPage.myFundingStatus.TransformPointDto;
import kcs.funding.fundingboost.domain.dto.response.member.MemberGenderStatusDto;
import kcs.funding.fundingboost.domain.security.resolver.Login;
import kcs.funding.fundingboost.domain.service.MemberService;
import org.springframework.web.bind.annotation.GetMapping;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/member")
public class MemberController {
    private final MemberService memberService;

    @GetMapping("/gender")
    public ResponseDto<MemberGenderStatusDto> getMemberGenderStatus(@Login Long memberId) {
        return ResponseDto.ok(memberService.getMemberGenderStatus(memberId));
    }

    /**
     * 포인트 전환하기
     */
    @PatchMapping("/point")
    public ResponseDto<CommonSuccessDto> exchangePoint(@Login Long memberId,
                                                       @RequestBody TransformPointDto transformPointDto) {
        return ResponseDto.ok(memberService.exchangePoint(transformPointDto));
    }

    @PatchMapping("/gender")
    public ResponseDto<CommonSuccessDto> updateMemberGender(@Login Long memberId,
                                                            @RequestBody UpdateMemberGenderDto updateMemberGenderDto) {
        return ResponseDto.ok(memberService.updateMemberGender(memberId, updateMemberGenderDto));
    }
}
