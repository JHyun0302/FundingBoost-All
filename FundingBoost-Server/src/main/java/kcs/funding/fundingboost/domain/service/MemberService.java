package kcs.funding.fundingboost.domain.service;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import java.util.Comparator;
import java.util.List;
import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.request.member.UpdateMemberGenderDto;
import kcs.funding.fundingboost.domain.dto.request.myPage.myFundingStatus.TransformPointDto;
import kcs.funding.fundingboost.domain.dto.response.member.MemberGenderStatusDto;
import kcs.funding.fundingboost.domain.entity.Funding;
import kcs.funding.fundingboost.domain.entity.FundingItem;
import kcs.funding.fundingboost.domain.entity.member.Member;
import kcs.funding.fundingboost.domain.entity.member.MemberGender;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.exception.ErrorCode;
import kcs.funding.fundingboost.domain.repository.MemberRepository;
import kcs.funding.fundingboost.domain.repository.funding.FundingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Timed("MemberService")
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MemberService {

    private final FundingRepository fundingRepository;
    private final MemberRepository memberRepository;

    @Counted("MemberService.exchangePoint")
    @Transactional
    public CommonSuccessDto exchangePoint(TransformPointDto transformPointDto) {
        Funding funding = fundingRepository.findMemberById(transformPointDto.fundingId());
        if (funding == null) {
            throw new CommonException(ErrorCode.NOT_FOUND_FUNDING);
        }
        Member member = funding.getMember();
        List<FundingItem> fundingItems = funding.getFundingItems();

        List<FundingItem> sortedFundingItems = fundingItems.stream()
                .sorted(Comparator.comparingInt(FundingItem::getItemSequence))
                .toList();

        int collectPrice = funding.getCollectPrice();
        for (FundingItem sortedFundingItem : sortedFundingItems) {
            if (!sortedFundingItem.isItemStatus()) {
                // 펀딩 완료된 상태라면
                collectPrice -= sortedFundingItem.getItem().getItemPrice();
            } else {
                member.plusPoint(collectPrice);
                sortedFundingItem.finishFundingItem(); // fundingItem을 finish 상태로 변경
                return CommonSuccessDto.fromEntity(true);
            }
        }
        return CommonSuccessDto.fromEntity(true);
    }

    public MemberGenderStatusDto getMemberGenderStatus(Long memberId) {
        if (memberId == null) {
            throw new CommonException(ErrorCode.NOT_FOUND_LOGIN_USER);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_MEMBER));

        return MemberGenderStatusDto.fromEntity(member);
    }

    @Transactional
    public CommonSuccessDto updateMemberGender(Long memberId, UpdateMemberGenderDto updateMemberGenderDto) {
        if (memberId == null) {
            throw new CommonException(ErrorCode.NOT_FOUND_LOGIN_USER);
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CommonException(ErrorCode.NOT_FOUND_MEMBER));
        MemberGender gender = updateMemberGenderDto.gender();

        if (gender == null || gender == MemberGender.UNKNOWN) {
            throw new CommonException(ErrorCode.INVALID_ARGUMENT);
        }

        member.changeGender(gender);
        return CommonSuccessDto.fromEntity(true);
    }
}
