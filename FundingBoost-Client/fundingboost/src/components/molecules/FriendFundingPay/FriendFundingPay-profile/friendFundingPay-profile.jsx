import React from 'react';
import defaultProfileImg from "../../../../assets/defaultProfile.svg";
import { toImageProxyUrl } from "../../../../utils/imageProxyUrl";


import './friendFundingPay-profile.scss';

const FriendFundingPayProfile = ({friendFundingPayData}) => {

    return (

        <div className="friend-funding-profile">
            <img className="friend-funding-profile-image" alt="Ellipse"
                 src={toImageProxyUrl(friendFundingPayData.friendProfile || defaultProfileImg)}/>
            <div className="friend-funding-profile-copy">
                <div className="friend-funding-profile-name">{friendFundingPayData.friendName}</div>
                <div className="friend-funding-profile-text">님에게 펀딩하기</div>
            </div>
        </div>

    );
};

export default FriendFundingPayProfile;
