import React, { useState } from 'react';
import "./friendFundingDetail-friendFunding.scss";
import FriendFundingDetailMoneyButtonGroup from "../button/friendFundingDetail-moneybuttonGroup/friendFundingDetail-moneybuttonGroup";
import GaugeBar from "../gauge-bar/gauge-bar";
import FriendToFundingBtn from "../button/FriendToFundingBtn/FriendToFundingBtn";


export default function FriendFundingDetailFriendToFunding({ friendFundingDetailData }) {
    const [fundingAmount, setFundingAmount] = useState(0);

    const handleFundingAmountChange = (amount) => {
        console.log("Funding amount changed:", amount);
        setFundingAmount(amount);
    };

    // 펀딩 버튼 클릭 시 실행
    const handleFriendToFundingBtnClick = () => {
        console.log("Funding button clicked. Funding amount:", fundingAmount);
    };

    return (
        <div className="friendFundingDetailFriendToFunding">
            <div className="friendFundingDetail-FriendToFunding">
                <div className="friendFundingDetail-FriendToFunding-style1">
                    <div className="fundingToFriend-Text">{friendFundingDetailData?.data?.friendName}님께 펀딩하기</div>
                    <div className="MoneyButtonGroup">
                        <FriendFundingDetailMoneyButtonGroup onFundingAmountChange={handleFundingAmountChange} friendFundingDetailData ={friendFundingDetailData} friendFundingPercent={friendFundingDetailData?.data?.contributedPercent}/>
                    </div>
                </div>
                <div className="friendFundingDetail-FriendToFunding-style2">
                    <div className="friendFundingDetail-fundingInfo">
                        <div className='friendFundingDetail-dDayGroup'>
                            <div className="friendFundingDetail-dDay">{friendFundingDetailData?.data?.deadLineDate}</div>
                            <div className="friendFundingDetail-deadLine">{friendFundingDetailData?.data?.deadline}</div>
                        </div>
                        <div className="friendFundingDetail-totalPercent">{friendFundingDetailData?.data?.contributedPercent}%</div>
                    </div>
                    <div className="friendFundingDetail-gaugeBar">
                        <GaugeBar value={friendFundingDetailData?.data?.contributedPercent} />
                    </div>
                </div>
                <div className="friendFundingDetail-FriendToFunding-style3">
                    <div className="friendFundingDetail-payButton">
                        <FriendToFundingBtn onClick={handleFriendToFundingBtnClick} fundingAmount={fundingAmount}  />
                    </div>
                </div>
            </div>
        </div>
    );
};
