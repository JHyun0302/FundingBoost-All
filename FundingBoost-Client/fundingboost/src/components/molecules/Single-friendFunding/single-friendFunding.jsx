import React from 'react';
import './single-friendFunding.scss'
import { Link } from 'react-router-dom';
import FriendFundingProfileDday from "../../atoms/friendFunding-profile-Dday/friendFunding-profile-Dday";
import FriendFundingItemImg from "../../atoms/friendFunding-itemImg/friendFunding-itemImg";
import GaugeBar from "../../atoms/gauge-bar/gauge-bar";

const SingleFriendFunding = ({friendFundingData}) => {
    return (

        <div className="friendFunding">
            <div className="friendFunding-row">
                <div className="friendFunding-itemlist">
                    {friendFundingData?.data?.map((fundingData, index) => (
                        <article key={fundingData?.fundingId ?? index} className="friendFundingCard">
                            {fundingData?.badgeLabel && (
                                <span className={`friendFundingBadge ${fundingData?.badgeTone || 'hot'}`}>
                                    {fundingData.badgeLabel}
                                </span>
                            )}
                            <Link
                                to={`/friend-funding/detail/${fundingData.fundingId}`}
                                className="friendFunding-Background"
                            >

                                <div className="friendFunding-Background-left">
                                    <div key={index} className="friendFundingDetail">
                                        <FriendFundingProfileDday friendFundingData={fundingData}/>
                                        <FriendFundingItemImg friendFundingData={fundingData}/>
                                        <div className="friendFunding-gaugeBar">
                                            <GaugeBar value={fundingData.friendFundingPercent}/>
                                        </div>
                                        <div className="friendFundingCollectPrice">
                                            현재 모금액 {Number(fundingData?.collectPrice ?? 0).toLocaleString()}원
                                        </div>
                                    </div>
                                </div>
                                <div className="friendFunding-Background-right">
                                    <div className="friendFundingbtn"></div>
                                </div>

                            </Link>
                        </article>
                    ))}

                </div>

            </div>
        </div>

    );
};

export default SingleFriendFunding;
