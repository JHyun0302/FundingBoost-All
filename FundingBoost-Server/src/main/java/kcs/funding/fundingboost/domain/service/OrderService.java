package kcs.funding.fundingboost.domain.service;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.INVALID_ACCESS_URL;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_MEMBER;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_RESOURCE;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import kcs.funding.fundingboost.domain.dto.response.myPage.MyPageMemberDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.orderHistory.OrderHistoryContributorDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.orderHistory.OrderHistoryDetailDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.orderHistory.OrderHistoryDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.orderHistory.OrderHistoryItemDto;
import kcs.funding.fundingboost.domain.entity.Contributor;
import kcs.funding.fundingboost.domain.entity.OrderItem;
import kcs.funding.fundingboost.domain.entity.member.Member;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.repository.MemberRepository;
import kcs.funding.fundingboost.domain.repository.contributor.ContributorRepository;
import kcs.funding.fundingboost.domain.repository.orderItem.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Timed("OrderService")
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class OrderService {

    private final MemberRepository memberRepository;
    private final OrderItemRepository orderItemRepository;
    private final ContributorRepository contributorRepository;

    @Counted("OrderService.getOrderHistory")
    public OrderHistoryDto getOrderHistory(Long memberId) {
        List<OrderItem> orderItemList = orderItemRepository.findLastOrderByMemberId(memberId);

        if (!orderItemList.isEmpty()) {
            // 주문 목록이 존재하는 경우
            List<OrderHistoryItemDto> orderHistoryItemDtoList = orderItemList.stream()
                    .map(OrderHistoryItemDto::fromEntity)
                    .toList(); // orderItemDtoList 초기화

            Member member = orderItemList.get(0).getOrder().getMember();
            MyPageMemberDto myPageMemberDto = MyPageMemberDto.fromEntity(member);// memberDto 초기화
            return OrderHistoryDto.fromEntity(myPageMemberDto, orderHistoryItemDtoList);
        } else {
            // 주문 목록이 존재하지 않는 경우
            Member member = memberRepository.findById(memberId)
                    .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));
            MyPageMemberDto myPageMemberDto = MyPageMemberDto.fromEntity(member);// memberDto 초기화
            return OrderHistoryDto.fromEntity(myPageMemberDto, null);
        }

    }

    public OrderHistoryDetailDto getOrderHistoryDetail(Long orderItemId, Long memberId) {
        OrderItem orderItem = orderItemRepository.findOrderHistoryDetailById(orderItemId);
        if (orderItem == null) {
            throw new CommonException(NOT_FOUND_RESOURCE);
        }
        if (!orderItem.getOrder().getMember().getMemberId().equals(memberId)) {
            throw new CommonException(INVALID_ACCESS_URL);
        }

        List<OrderHistoryContributorDto> contributors = orderItem.getOrder().getSourceFundingId() == null
                ? List.of()
                : contributorRepository.findAllByFundingId(orderItem.getOrder().getSourceFundingId()).stream()
                        .map(OrderHistoryContributorDto::fromEntity)
                        .toList();

        return OrderHistoryDetailDto.fromEntity(orderItem, resolvePaymentLabel(orderItem), contributors);
    }

    private String resolvePaymentLabel(OrderItem orderItem) {
        boolean hasFunding = orderItem.getOrder().getFundingSupportedAmount() > 0;
        boolean hasPoint = orderItem.getOrder().getPointUsedAmount() > 0;
        boolean hasDirect = orderItem.getOrder().getDirectPaidAmount() > 0;

        if (hasFunding && hasPoint && hasDirect) {
            return "펀딩금 + 포인트 + 직접 결제";
        }
        if (hasFunding && hasPoint) {
            return "펀딩금 + 포인트 결제";
        }
        if (hasFunding && hasDirect) {
            return "펀딩금 + 직접 결제";
        }
        if (hasFunding) {
            return "펀딩금으로 결제";
        }
        if (hasPoint && hasDirect) {
            return "포인트 + 직접 결제";
        }
        if (hasPoint) {
            return "포인트 결제";
        }
        return "직접 결제";
    }
}
