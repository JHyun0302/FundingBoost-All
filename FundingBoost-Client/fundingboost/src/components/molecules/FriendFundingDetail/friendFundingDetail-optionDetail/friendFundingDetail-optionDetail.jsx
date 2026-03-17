import React from 'react';
import './friendFundingDetail-optionDetail.scss';
import FriendFundingDetailFriendFunding from '../../../atoms/friendFundingDetail-friendFunding/friendFundingDetail-friendFunding';
import FriendFundingDetailProfileMessage from '../../../atoms/friendFundingDetail-profileMessage/friendFundingDetail-profileMessage';

const FriendFundingDetailOptionDetail = ({ friendFundingDetailData }) => {
    return (
        <div className="friendFundingDetail-optionDetail">
            <div className="friendFundingDetail-optionDetail1">
                <FriendFundingDetailProfileMessage friendFundingDetailData={friendFundingDetailData} />
            </div>
            <div className="friendFundingDetail-optionDetail2">
                <FriendFundingDetailFriendFunding friendFundingDetailData={friendFundingDetailData} />
            </div>
        </div>
    );
};

export default FriendFundingDetailOptionDetail;
