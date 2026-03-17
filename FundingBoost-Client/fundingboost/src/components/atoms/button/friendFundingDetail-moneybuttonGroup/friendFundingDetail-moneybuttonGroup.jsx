import React, { useState } from 'react';
import './friendFundingDetail-moneybuttonGroup.scss'

const FriendFundingDetailMoneybuttonGroup = ({ onFundingAmountChange, friendFundingDetailData, friendFundingPercent  }) => {
    const [fundingAmount, setFundingAmount] = useState(0);

    // 버튼을 통한 금액 입력
    const handleMoneyButtonClick = (amount) => {
        const newAmount = Math.min(parseInt(fundingAmount) + amount, friendFundingDetailData?.data?.leftPrice);
        setFundingAmount(newAmount);
        onFundingAmountChange(newAmount);
    };


    //사용자가 직접 금액 입력
    const handleMoneyInputChange = (event) => {
        const inputValue = event.target.value;
        const numericValue = inputValue.replace(/\D/g, '');  //문자입력제한
        const newAmount = Math.min(numericValue === "" ? 0 : parseInt(numericValue), friendFundingDetailData?.data?.leftPrice);
        setFundingAmount(newAmount);
        onFundingAmountChange(newAmount);
    };


    //숫자 형식 천단위 "," 표시
    const formattedFundingAmount = fundingAmount ? parseInt(fundingAmount).toLocaleString() : "펀딩할 금액을 입력해 주세요";

    return (
        <div className="fundingToFriend-buttonGroup">
            <input type="text" className="fundingToFriend-textBox"
                   placeholder={friendFundingPercent === 100? "펀딩이 종료되었습니다. :) " : "펀딩할 금액을 입력해 주세요. "}
                   value={formattedFundingAmount === "펀딩할 금액을 입력해 주세요" ? "" : formattedFundingAmount}
                   onChange={handleMoneyInputChange} disabled={friendFundingPercent === 100}/>
            {friendFundingPercent !== 100 ? (
                //펀딩이 진행 중인 경우
                    <div className="friendFundingDetail-moneybuttonGroup">
                        <div className="money">
                            <button className="moneyButton" onClick={() => handleMoneyButtonClick(10000)}>+ 1만</button>
                        </div>
                        <div className="money">
                            <button className="moneyButton" onClick={() => handleMoneyButtonClick(50000)}>+ 5만</button>
                        </div>
                        <div className="money">
                            <button className="moneyButton" onClick={() => handleMoneyButtonClick(100000)}>+ 10만
                            </button>
                        </div>
                        <div className="money">
                            <button className="moneyButton"
                                    onClick={() => handleMoneyButtonClick(friendFundingDetailData?.data?.leftPrice)}>+
                                전액
                            </button>
                        </div>
                    </div>

            ) : (
                // 펀딩이 100%인 경우
                    <div className="friendFundingDetail-moneybuttonGroup">
                        <div className="NonMoney">
                            <button className="NonMoneyButton">+ 1만</button>
                        </div>
                        <div className="NonMoney">
                            <button className="NonMoneyButton">+ 5만</button>
                        </div>
                        <div className="NonMoney">
                            <button className="NonMoneyButton">+ 10만</button>
                        </div>
                        <div className="NonMoney">
                            <button className="NonMoneyButton">+ 전액</button>
                        </div>
                    </div>

            )}


        </div>
    );
};

export default FriendFundingDetailMoneybuttonGroup;