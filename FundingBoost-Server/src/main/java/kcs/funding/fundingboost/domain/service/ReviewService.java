package kcs.funding.fundingboost.domain.service;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_MEMBER;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import kcs.funding.fundingboost.domain.dto.response.myPage.MyPageMemberDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.review.MyReviewHistoryDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.review.MyReviewItemDto;
import kcs.funding.fundingboost.domain.entity.Review;
import kcs.funding.fundingboost.domain.entity.member.Member;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.repository.MemberRepository;
import kcs.funding.fundingboost.domain.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Timed("ReviewService")
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final MemberRepository memberRepository;
    private final ReviewRepository reviewRepository;

    @Counted("ReviewService.getMyReviewHistory")
    public MyReviewHistoryDto getMyReviewHistory(Long memberId) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));

        List<Review> reviews = reviewRepository.findAllByMemberIdOrderByReviewIdDesc(memberId);
        List<MyReviewItemDto> myReviewItemDtoList = reviews.stream()
                .map(MyReviewItemDto::fromEntity)
                .toList();

        return MyReviewHistoryDto.fromEntity(MyPageMemberDto.fromEntity(member), myReviewItemDtoList);
    }
}
