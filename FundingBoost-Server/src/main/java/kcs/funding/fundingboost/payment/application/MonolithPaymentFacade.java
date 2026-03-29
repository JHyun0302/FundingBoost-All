package kcs.funding.fundingboost.payment.application;

import jakarta.servlet.http.HttpServletRequest;
import kcs.funding.fundingboost.domain.dto.common.CommonSuccessDto;
import kcs.funding.fundingboost.domain.dto.request.pay.friendFundingPay.FriendPayBarcodeConsumeDto;
import kcs.funding.fundingboost.domain.dto.request.pay.friendFundingPay.FriendPayProcessDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.ItemPayNowDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.MyPayDto;
import kcs.funding.fundingboost.domain.dto.request.pay.myPay.PayRemainDto;
import kcs.funding.fundingboost.domain.dto.response.pay.friendFundingPay.FriendFundingPayingDto;
import kcs.funding.fundingboost.domain.dto.response.pay.friendFundingPay.FriendPayBarcodeIssueDto;
import kcs.funding.fundingboost.domain.dto.response.pay.friendFundingPay.FriendPayBarcodeVerifyDto;
import kcs.funding.fundingboost.domain.dto.response.pay.myPay.MyFundingPayViewDto;
import kcs.funding.fundingboost.domain.dto.response.pay.myPay.MyOrderPayViewDto;
import kcs.funding.fundingboost.domain.service.pay.FriendPayService;
import kcs.funding.fundingboost.domain.service.pay.MyPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class MonolithPaymentFacade implements PaymentFacade {

    private final MyPayService myPayService;
    private final FriendPayService friendPayService;

    @Value("${app.pay.barcode-verify-base-url:}")
    private String barcodeVerifyBaseUrl;

    @Override
    public MyOrderPayViewDto getOrderPayView(Long memberId) {
        return myPayService.myOrderPayView(memberId);
    }

    @Override
    public MyFundingPayViewDto getFundingPayView(Long fundingItemId, Long memberId) {
        return myPayService.myFundingPayView(fundingItemId, memberId);
    }

    @Override
    public CommonSuccessDto payOrder(Long memberId, MyPayDto paymentDto, String idempotencyKey) {
        return myPayService.payMyItem(paymentDto, memberId, idempotencyKey);
    }

    @Override
    public CommonSuccessDto payOrderNow(Long memberId, ItemPayNowDto itemPayNowDto, String idempotencyKey) {
        return myPayService.payMyItemNow(itemPayNowDto, memberId, idempotencyKey);
    }

    @Override
    public CommonSuccessDto payFunding(Long memberId, Long fundingItemId, PayRemainDto payRemainDto, String idempotencyKey) {
        return myPayService.payMyFunding(fundingItemId, payRemainDto, memberId, idempotencyKey);
    }

    @Override
    public FriendFundingPayingDto getFriendFundingPay(Long memberId, Long fundingId) {
        return friendPayService.getFriendFundingPay(fundingId, memberId);
    }

    @Override
    public CommonSuccessDto fundFriend(Long memberId, Long fundingId, FriendPayProcessDto friendPayProcessDto) {
        return friendPayService.fund(memberId, fundingId, friendPayProcessDto);
    }

    @Override
    public FriendPayBarcodeIssueDto issueFriendFundingBarcodeToken(
            Long memberId,
            Long fundingId,
            FriendPayProcessDto friendPayProcessDto,
            HttpServletRequest request
    ) {
        FriendPayBarcodeIssueDto issued = friendPayService.issueBarcodeToken(memberId, fundingId, friendPayProcessDto);
        String verifyUrl = buildVerifyUrl(request, issued.token());

        return FriendPayBarcodeIssueDto.builder()
                .token(issued.token())
                .barcodeValue(issued.token())
                .verifyUrl(verifyUrl)
                .expiresAt(issued.expiresAt())
                .usingPoint(issued.usingPoint())
                .fundingPrice(issued.fundingPrice())
                .build();
    }

    @Override
    public FriendPayBarcodeVerifyDto verifyFriendFundingBarcodeToken(String token) {
        return friendPayService.verifyBarcodeToken(token);
    }

    @Override
    public CommonSuccessDto consumeFriendFundingBarcodeToken(Long memberId, Long fundingId, FriendPayBarcodeConsumeDto consumeDto) {
        return friendPayService.fundWithBarcodeToken(memberId, fundingId, consumeDto);
    }

    private String buildVerifyUrl(HttpServletRequest request, String token) {
        if (barcodeVerifyBaseUrl != null && !barcodeVerifyBaseUrl.isBlank()) {
            String normalizedBaseUrl = barcodeVerifyBaseUrl.endsWith("/")
                    ? barcodeVerifyBaseUrl.substring(0, barcodeVerifyBaseUrl.length() - 1)
                    : barcodeVerifyBaseUrl;
            return normalizedBaseUrl + "/api/v1/pay/friends/barcode-token/" + token;
        }
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/api/v1/pay/friends/barcode-token/{token}")
                .replaceQuery(null)
                .buildAndExpand(token)
                .toUriString();
    }
}
