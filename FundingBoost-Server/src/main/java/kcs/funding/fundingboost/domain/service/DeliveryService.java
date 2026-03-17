package kcs.funding.fundingboost.domain.service;

import static kcs.funding.fundingboost.domain.exception.ErrorCode.DELIVERY_IN_USE;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_DELIVERY;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_FOUND_MEMBER;
import static kcs.funding.fundingboost.domain.exception.ErrorCode.NOT_MATCH_USER;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import java.util.List;
import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.request.myPage.deliveryManage.CreateDeliveryRequestDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.MyPageMemberDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.deliveryManage.MyPageDeliveryDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.deliveryManage.MyPageDeliveryManageDto;
import kcs.funding.fundingboost.domain.entity.Delivery;
import kcs.funding.fundingboost.domain.entity.member.Member;
import kcs.funding.fundingboost.domain.exception.CommonException;
import kcs.funding.fundingboost.domain.repository.DeliveryRepository;
import kcs.funding.fundingboost.domain.repository.MemberRepository;
import kcs.funding.fundingboost.domain.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Timed("DeliveryService")
@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class DeliveryService {
    private final DeliveryRepository deliveryRepository;
    private final MemberRepository memberRepository;
    private final OrderRepository orderRepository;


    @Counted("DeliveryService.getMyDeliveryManageList")
    public MyPageDeliveryManageDto getMyDeliveryManageList(Long memberId) {
        List<Delivery> deliveryList = deliveryRepository.findAllByMemberId(memberId);

        Member member = memberRepository.findById(memberId).orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));

        MyPageMemberDto myPageMemberDto = MyPageMemberDto.fromEntity(member);

        List<MyPageDeliveryDto> myPageDeliveryDtoList = deliveryList.stream()
                .map(MyPageDeliveryDto::fromEntity).toList();

        return MyPageDeliveryManageDto.fromEntity(myPageMemberDto, myPageDeliveryDtoList);
    }

    @Transactional
    public CommonSuccessDto createDelivery(Long memberId, CreateDeliveryRequestDto requestDto) {
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_MEMBER));

        Delivery delivery = Delivery.createDelivery(
                requestDto.address(),
                requestDto.phoneNumber(),
                requestDto.customerName(),
                requestDto.postalCode(),
                requestDto.deliveryMemo(),
                member
        );

        deliveryRepository.save(delivery);
        return CommonSuccessDto.fromEntity(true);
    }

    @Transactional
    public CommonSuccessDto deleteDelivery(Long memberId, Long deliveryId) {
        Delivery delivery = deliveryRepository.findById(deliveryId)
                .orElseThrow(() -> new CommonException(NOT_FOUND_DELIVERY));

        if (!delivery.getMember().getMemberId().equals(memberId)) {
            throw new CommonException(NOT_MATCH_USER);
        }

        if (orderRepository.existsByDelivery_DeliveryId(deliveryId)) {
            throw new CommonException(DELIVERY_IN_USE);
        }

        deliveryRepository.delete(delivery);
        return CommonSuccessDto.fromEntity(true);
    }
}
