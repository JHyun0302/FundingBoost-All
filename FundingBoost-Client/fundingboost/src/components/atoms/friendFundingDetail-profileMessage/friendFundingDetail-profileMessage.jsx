import React from 'react';
import "./friendFundingDetail-profileMessage.scss";
import img2 from "../../../assets/airplane.png";
import defaultProfileImg from "../../../assets/defaultProfile.svg";
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

export default function FriendFundingDetailFriendProfile ({friendFundingDetailData}) {
    const contributorList = friendFundingDetailData?.data?.contributorList ?? [];
    const uniqueContributorList = contributorList.filter((contributor, index, list) => {
        return index === list.findIndex((target) =>
            target.contributorName === contributor.contributorName
            && target.contributorProfileImgUrl === contributor.contributorProfileImgUrl
        );
    });

    return (
        <div className="friendFundingDetail-FriendProfile">
            <div className="friendFundingDetail-Profile">
                <div className='friendProfileInfo'>
                    <img className="friendFundingDetail-friendProfileImage" alt="FriendProfile"
                         src={toImageProxyUrl(friendFundingDetailData?.data?.friendProfileImgUrl || defaultProfileImg)}/>
                    <div className="friendFundingDetail-friendName">{friendFundingDetailData?.data?.friendName}</div>
                    <div className="friendFundingDetail-fundingTag">{friendFundingDetailData?.data?.fundingTag}</div>
                </div>
                <div className="fundingMessage-triangle"></div>
                <div className="fundingMessageBackSpace">

                    <div className="fundingMessage">{friendFundingDetailData?.data?.fundingMessage}</div>
                </div>
                <div className='fundingFriendsTitle'>
                    <img className="paperAirplane" alt="paperAirplane" src={img2}/>
                    <div className="fundingFriendsIntro">{friendFundingDetailData?.data?.friendName}님에게 펀딩한 친구들</div>
                </div>
                <div className="friendFundingDetail-fundingFriendlist">
                    <div className="friendFundingDetail-fundingFriends">
                        {uniqueContributorList.map((contributor, index) => (
                            <div
                                className="friendFundingDetail-fundingFriends-img"
                                key={`${contributor.contributorName}-${contributor.contributorProfileImgUrl}-${index}`}
                            >
                                <img className="fundingFriends-img" alt="Ellipse"
                                    src={toImageProxyUrl(contributor.contributorProfileImgUrl || defaultProfileImg)}/>
                            <div className="friendFundingDetail-friendName">{contributor.contributorName}</div>
                            </div>
                        ))}
                    </div>
                </div>

            </div>
        </div>
    );
};
