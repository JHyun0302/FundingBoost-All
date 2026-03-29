package kcs.funding.fundingboost.payment.api;

import jakarta.servlet.http.HttpServletRequest;
import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.dto.request.pay.friendFundingPay.FriendPayBarcodeConsumeDto;
import kcs.funding.fundingboost.domain.dto.request.pay.friendFundingPay.FriendPayProcessDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.ItemPayNowDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.MyPayDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.PayRemainDto;
import kcs.funding.fundingboost.domain.dto.response.pay.friendFundingPay.FriendPayBarcodeIssueDto;
import kcs.funding.fundingboost.domain.security.resolver.Login;
import kcs.funding.fundingboost.payment.application.PaymentFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/payment/commands")
public class InternalPaymentCommandController {

    private final PaymentFacade paymentFacade;

    @PostMapping("/order")
    public ResponseDto<CommonSuccessDto> payOrder(
            @Login Long memberId,
            @RequestBody MyPayDto paymentDto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ResponseDto.ok(paymentFacade.payOrder(memberId, paymentDto, idempotencyKey));
    }

    @PostMapping("/order/now")
    public ResponseDto<CommonSuccessDto> payOrderNow(
            @Login Long memberId,
            @RequestBody ItemPayNowDto itemPayNowDto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ResponseDto.ok(paymentFacade.payOrderNow(memberId, itemPayNowDto, idempotencyKey));
    }

    @PostMapping("/funding/{fundingItemId}")
    public ResponseDto<CommonSuccessDto> payFunding(
            @Login Long memberId,
            @PathVariable("fundingItemId") Long fundingItemId,
            @RequestBody PayRemainDto payRemainDto,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        return ResponseDto.ok(paymentFacade.payFunding(memberId, fundingItemId, payRemainDto, idempotencyKey));
    }

    @PostMapping("/friends/{fundingId}")
    public ResponseDto<CommonSuccessDto> fundFriend(
            @Login Long memberId,
            @PathVariable("fundingId") Long fundingId,
            @RequestBody FriendPayProcessDto friendPayProcessDto
    ) {
        return ResponseDto.ok(paymentFacade.fundFriend(memberId, fundingId, friendPayProcessDto));
    }

    @PostMapping("/friends/{fundingId}/barcode-token")
    public ResponseDto<FriendPayBarcodeIssueDto> issueFriendFundingBarcodeToken(
            @Login Long memberId,
            @PathVariable("fundingId") Long fundingId,
            @RequestBody FriendPayProcessDto friendPayProcessDto,
            HttpServletRequest request
    ) {
        return ResponseDto.ok(
                paymentFacade.issueFriendFundingBarcodeToken(memberId, fundingId, friendPayProcessDto, request)
        );
    }

    @PostMapping("/friends/{fundingId}/barcode-token/consume")
    public ResponseDto<CommonSuccessDto> consumeFriendFundingBarcodeToken(
            @Login Long memberId,
            @PathVariable("fundingId") Long fundingId,
            @RequestBody FriendPayBarcodeConsumeDto consumeDto
    ) {
        return ResponseDto.ok(paymentFacade.consumeFriendFundingBarcodeToken(memberId, fundingId, consumeDto));
    }
}
