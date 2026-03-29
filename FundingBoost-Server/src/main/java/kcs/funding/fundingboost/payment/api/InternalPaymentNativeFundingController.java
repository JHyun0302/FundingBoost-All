package kcs.funding.fundingboost.payment.api;

import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.PayRemainDto;
import kcs.funding.fundingboost.domain.security.resolver.Login;
import kcs.funding.fundingboost.payment.api.nativeflow.FundingFinalizeRequestDto;
import kcs.funding.fundingboost.payment.api.nativeflow.OrderFinalizeResultDto;
import kcs.funding.fundingboost.payment.api.nativeflow.OrderPreparationDto;
import kcs.funding.fundingboost.payment.application.nativeflow.InternalPaymentOrderFlowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/payment/native")
public class InternalPaymentNativeFundingController {

    private final InternalPaymentOrderFlowService internalPaymentOrderFlowService;

    @PostMapping("/funding/{fundingItemId}/prepare")
    public ResponseDto<OrderPreparationDto> prepareFunding(
            @Login Long memberId,
            @PathVariable("fundingItemId") Long fundingItemId,
            @RequestBody PayRemainDto payRemainDto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ResponseDto.ok(
                internalPaymentOrderFlowService.prepareFunding(memberId, fundingItemId, payRemainDto, idempotencyKey)
        );
    }

    @PostMapping("/funding/{fundingItemId}/finalize")
    public ResponseDto<OrderFinalizeResultDto> finalizeFunding(
            @Login Long memberId,
            @PathVariable("fundingItemId") Long fundingItemId,
            @RequestBody FundingFinalizeRequestDto finalizeRequestDto
    ) {
        return ResponseDto.ok(
                internalPaymentOrderFlowService.finalizeFunding(
                        memberId,
                        fundingItemId,
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
