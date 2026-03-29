package kcs.funding.fundingboost.payment.api;

import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.ItemPayNowDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.MyPayDto;
import kcs.funding.fundingboost.domain.security.resolver.Login;
import kcs.funding.fundingboost.payment.api.nativeflow.OrderFinalizeRequestDto;
import kcs.funding.fundingboost.payment.api.nativeflow.OrderFinalizeResultDto;
import kcs.funding.fundingboost.payment.api.nativeflow.OrderNowFinalizeRequestDto;
import kcs.funding.fundingboost.payment.api.nativeflow.OrderPreparationDto;
import kcs.funding.fundingboost.payment.application.nativeflow.InternalPaymentOrderFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/payment/native")
public class InternalPaymentNativeOrderController {

    private final InternalPaymentOrderFlowService internalPaymentOrderFlowService;

    @PostMapping("/order/prepare")
    public ResponseDto<OrderPreparationDto> prepareOrder(
            @Login Long memberId,
            @RequestBody MyPayDto myPayDto
    ) {
        return ResponseDto.ok(internalPaymentOrderFlowService.prepareOrder(memberId, myPayDto));
    }

    @PostMapping("/order/finalize")
    public ResponseDto<OrderFinalizeResultDto> finalizeOrder(
            @Login Long memberId,
            @RequestBody OrderFinalizeRequestDto finalizeRequestDto
    ) {
        return ResponseDto.ok(
                internalPaymentOrderFlowService.finalizeOrder(
                        memberId,
                        finalizeRequestDto.payload(),
                        finalizeRequestDto.paymentIntentKey(),
                        finalizeRequestDto.totalAmount(),
                        finalizeRequestDto.pointAmount(),
                        finalizeRequestDto.pgAmount(),
                        finalizeRequestDto.fundingSupportedAmount()
                )
        );
    }

    @PostMapping("/order/now/prepare")
    public ResponseDto<OrderPreparationDto> prepareOrderNow(
            @Login Long memberId,
            @RequestBody ItemPayNowDto itemPayNowDto
    ) {
        return ResponseDto.ok(internalPaymentOrderFlowService.prepareOrderNow(memberId, itemPayNowDto));
    }

    @PostMapping("/order/now/finalize")
    public ResponseDto<OrderFinalizeResultDto> finalizeOrderNow(
            @Login Long memberId,
            @RequestBody OrderNowFinalizeRequestDto finalizeRequestDto
    ) {
        return ResponseDto.ok(
                internalPaymentOrderFlowService.finalizeOrderNow(
                        memberId,
                        finalizeRequestDto.payload(),
                        finalizeRequestDto.paymentIntentKey(),
                        finalizeRequestDto.totalAmount(),
                        finalizeRequestDto.pointAmount(),
                        finalizeRequestDto.pgAmount(),
                        finalizeRequestDto.fundingSupportedAmount()
                )
        );
    }
}
