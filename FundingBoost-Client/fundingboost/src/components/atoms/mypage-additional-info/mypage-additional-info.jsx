import React from 'react';
import './mypage-additional-info.scss';
import Gauge from "../../atoms/mypage-myfunding-gauge/mypage-myfunding-gauge"
import JoinFriend from "../MyPageMyfundingJoinFriendGroup/MyPageMyfundingJoinFriendGroup";

export default function MyfundingAdditionalPane({ participateFriendDtoList, totalPercent }) {

    return (
        <div className="mypage-right-additional-pane-containter">
            <div className="mypage-additional-wrapper">
                <div className="mypage-additional-container">
                    <div className="mypage-myfunding-additional-title-01">전체 진행률</div>
                    <div className="mypage-myfunding-totalper-wrapper">{totalPercent}%</div>
                    <Gauge totalPercent={totalPercent}/>
                    <div className="mypage-myfunding-additional-title-02">내 펀딩에 참여한 친구</div>
                    <JoinFriend participateFriendDtoList={participateFriendDtoList}/>
                </div>
            </div>
        </div>
    );
}
