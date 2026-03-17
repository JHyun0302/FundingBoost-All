import React from 'react';
import './FriendFundingPay-CurrentPay.scss';

const FriendFundingPayCurrentPay = ({friendFundingPayData,fundingAmount,usePoints,onUpdateFinalPrice}) => {

    const finalPrice = () => {
        const numericUsePoints = usePoints ? Number(usePoints) : 0;
        const finalTotalPrice=(fundingAmount - numericUsePoints).toLocaleString();

        onUpdateFinalPrice(finalTotalPrice);
        return finalTotalPrice
    }
    return (
        <div className="friend-funding-price-current-pay">
            <div className="friend-funding-price-current-pay-fixed-text">결제 금액</div>
            <div className="friend-funding-price-current-pay-div">
                <div className="friend-funding-price-current-pay-text">
                    <div>펀딩한 금액</div>
                    <div>{fundingAmount.toLocaleString()}</div>
                </div>
                <div className="friend-funding-price-current-pay-text">
                    <div>사용 포인트</div>
                    <div>{(Number(usePoints) || 0).toLocaleString()}</div>
                </div>
                <div className="friend-funding-price-current-pay-text">
                    <div>결제할 금액</div>
                    <div>{finalPrice()}</div>

                </div>
            </div>
        </div>
    );
};

export default FriendFundingPayCurrentPay;
