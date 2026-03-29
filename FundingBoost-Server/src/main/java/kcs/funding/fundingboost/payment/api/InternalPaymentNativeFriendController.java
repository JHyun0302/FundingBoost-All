package kcs.funding.fundingboost.payment.api;

import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.global.ResponseDto;
import kcs.funding.fundingboost.domain.dto.request.pay.friendFundingPay.FriendPayBarcodeConsumeDto;
import kcs.funding.fundingboost.domain.dto.request.pay.friendFundingPay.FriendPayProcessDto;
import kcs.funding.fundingboost.domain.dto.response.pay.friendFundingPay.FriendPayBarcodeIssueDto;
import kcs.funding.fundingboost.domain.security.resolver.Login;
import kcs.funding.fundingboost.domain.service.pay.FriendPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/internal/payment/native/friends")
public class InternalPaymentNativeFriendController {

    private final FriendPayService friendPayService;

    @PostMapping("/{fundingId}")
    public ResponseDto<CommonSuccessDto> fundFriend(
            @Login Long memberId,
            @PathVariable("fundingId") Long fundingId,
            @RequestBody FriendPayProcessDto friendPayProcessDto
    ) {
        return ResponseDto.ok(friendPayService.fund(memberId, fundingId, friendPayProcessDto));
    }

    @PostMapping("/{fundingId}/barcode-token")
    public ResponseDto<FriendPayBarcodeIssueDto> issueFriendFundingBarcodeToken(
            @Login Long memberId,
            @PathVariable("fundingId") Long fundingId,
            @RequestBody FriendPayProcessDto friendPayProcessDto
    ) {
        return ResponseDto.ok(friendPayService.issueBarcodeToken(memberId, fundingId, friendPayProcessDto));
    }

    @PostMapping("/{fundingId}/barcode-token/consume")
    public ResponseDto<CommonSuccessDto> consumeFriendFundingBarcodeToken(
            @Login Long memberId,
            @PathVariable("fundingId") Long fundingId,
            @RequestBody FriendPayBarcodeConsumeDto consumeDto
    ) {
        return ResponseDto.ok(friendPayService.fundWithBarcodeToken(memberId, fundingId, consumeDto));
    }
}
