import React from 'react';
import './friendFundingPay-Price.scss';
const FriendFundingPayPrice = ({friendFundingPayData}) => {
    // 최대 펀딩 가능 금액
    const leftPrice = () => {
        if (friendFundingPayData) {
            return (friendFundingPayData.totalPrice - friendFundingPayData.presentPrice).toLocaleString();
        }
        return 0;

    };
    return (
        <div className="friend-funding-total-price">
            <div className="friend-funding-total-price-row">
                <div className="friend-funding-total-price-first-text">친구의 펀딩 총 금액</div>
                <div
                    className="friend-funding-total-price-second-text">{friendFundingPayData.totalPrice.toLocaleString()}</div>
            </div>
            <div className="friend-funding-total-price-row">
                <div className="friend-funding-total-price-first-text">현재 펀딩완료 금액</div>
                <div
                    className="friend-funding-total-price-second-text">{friendFundingPayData.presentPrice.toLocaleString()}</div>
            </div>
            <div className="friend-funding-total-price-row">
                <div className="friend-funding-total-price-first-text">최대 펀딩 가능 금액</div>
                <div className="friend-funding-total-price-second-text">{leftPrice()}</div>
            </div>
        </div>
    );
};

export default FriendFundingPayPrice;