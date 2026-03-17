import React from 'react';
import './MyPageMyfundingJoinFriend.scss';
import defaultprofileimg from "../../../assets/defaultProfile.svg";
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

export default function MyPageFundingJoinFriend({ friend }) {
    const { participateNickname, participatePrice, participateProfileImgUrl } = friend;

    return (
        <div className="myPageFundingJoinFriendView">
            <img
                className="myPageFundingJoinFriendEllipse"
                alt="profileimg"
                src={toImageProxyUrl(participateProfileImgUrl || defaultprofileimg)}
            />
            <div className="myPageFundingJoinFriendEllipseFunny">
                <div className="myPageFundingJoinFriendDiv">{participateNickname}</div>
                <div className="myPageFundingJoinFriendText-wrapper">{participatePrice.toLocaleString()} 원</div>
            </div>
        </div>
    );
};
