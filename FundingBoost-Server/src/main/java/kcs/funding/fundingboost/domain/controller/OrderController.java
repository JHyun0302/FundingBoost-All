package kcs.funding.fundingboost.domain.controller;

import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.orderHistory.OrderHistoryDetailDto;
import kcs.funding.fundingboost.domain.dto.response.myPage.orderHistory.OrderHistoryDto;
import kcs.funding.fundingboost.domain.security.resolver.Login;
import kcs.funding.fundingboost.domain.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/order")
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/history")
    public ResponseDto<OrderHistoryDto> orderHistory(@Login Long memberId) {
        return ResponseDto.ok(orderService.getOrderHistory(memberId));
    }

    @GetMapping("/history/{orderItemId}")
    public ResponseDto<OrderHistoryDetailDto> orderHistoryDetail(@Login Long memberId,
                                                                 @PathVariable Long orderItemId) {
        return ResponseDto.ok(orderService.getOrderHistoryDetail(orderItemId, memberId));
    }
}
