package kcs.funding.fundingboost.domain.controller;

import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.dto.request.myPage.deliveryManage.CreateDeliveryRequestDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.deliveryManage.MyPageDeliveryManageDto;
import kcs.funding.fundingboost.domain.security.resolver.Login;
import kcs.funding.fundingboost.domain.service.DeliveryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    /**
     * 배송지 관리 조회
     */
    @GetMapping("/api/v1/delivery")
    public ResponseDto<MyPageDeliveryManageDto> viewMyDeliveryManagement(@Login Long memberId) {
        return ResponseDto.ok(deliveryService.getMyDeliveryManageList(memberId));
    }

    @PostMapping("/api/v1/delivery")
    public ResponseDto<CommonSuccessDto> createDelivery(@Login Long memberId,
                                                        @RequestBody CreateDeliveryRequestDto requestDto) {
        return ResponseDto.created(deliveryService.createDelivery(memberId, requestDto));
    }

    @DeleteMapping("/api/v1/delivery/{deliveryId}")
    public ResponseDto<CommonSuccessDto> deleteDelivery(@Login Long memberId,
                                                        @PathVariable Long deliveryId) {
        return ResponseDto.ok(deliveryService.deleteDelivery(memberId, deliveryId));
    }
}
