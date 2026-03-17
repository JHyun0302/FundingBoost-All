import React from 'react';
import hweng from '../../../../assets/hweng.svg';
import './mainFriendNoFunding.scss';

const MainFriendNoFunding = () => {
    return (

        <div className="mainFriendNoFunding">
            <div className="mainFriendNoFunding-text">
                친구를 위한 작은 선물! 지금 펀딩에 참여해보세요
            </div>
            <div className ="friendNoFunding-contents">
                <div className="friendNoFunding-hweng">
                    <img className="hwengImg" src={hweng}/>
                    <div className="hwengText">아직 펀딩중인 친구가 없어요</div>
                </div>
            </div>
        </div>
    );
};

export default MainFriendNoFunding;