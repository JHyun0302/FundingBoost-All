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

public interface PaymentFacade {

    MyOrderPayViewDto getOrderPayView(Long memberId);

    MyFundingPayViewDto getFundingPayView(Long fundingItemId, Long memberId);

    CommonSuccessDto payOrder(Long memberId, MyPayDto paymentDto, String idempotencyKey);

    CommonSuccessDto payOrderNow(Long memberId, ItemPayNowDto itemPayNowDto, String idempotencyKey);

    CommonSuccessDto payFunding(Long memberId, Long fundingItemId, PayRemainDto payRemainDto, String idempotencyKey);

    FriendFundingPayingDto getFriendFundingPay(Long memberId, Long fundingId);

    CommonSuccessDto fundFriend(Long memberId, Long fundingId, FriendPayProcessDto friendPayProcessDto);

    FriendPayBarcodeIssueDto issueFriendFundingBarcodeToken(
            Long memberId,
            Long fundingId,
            FriendPayProcessDto friendPayProcessDto,
            HttpServletRequest request
    );

    FriendPayBarcodeVerifyDto verifyFriendFundingBarcodeToken(String token);

    CommonSuccessDto consumeFriendFundingBarcodeToken(Long memberId, Long fundingId, FriendPayBarcodeConsumeDto consumeDto);
}
