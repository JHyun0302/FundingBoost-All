import React from 'react';
import ShoppingNowBtn from '../../../atoms/button/ShoppingNowBtn/shoppingNowBtn';
import './friendNonFunding.scss';
import hweng from '../../../../assets/hweng.svg';

const FriendNonFunding = () => {
    return (
        <div className="friendNonFunding">
            <hr style={{border: '1px solid', color: 'black', width: '100%'}}/>
            <div className="friendNonFundingText">
                <img src={hweng} style={{width: '140px'}}/>
                <div className="friendNonFunding-Title">아직 펀딩중인 친구가 없어요</div>
            </div>

            <hr style={{border: '1px solid', color: 'black', width: '100%'}}/>

        </div>
    );
};

export default FriendNonFunding;