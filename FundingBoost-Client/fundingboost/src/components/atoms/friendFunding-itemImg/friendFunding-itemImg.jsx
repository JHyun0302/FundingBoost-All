import React from 'react';
import './friendFunding-itemImg.scss';
import NonItemImg from "../../../assets/nonItemImg.svg";
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

const FriendFundingItemImg = ({friendFundingData}) => {
    const fundingItems = Array.isArray(friendFundingData?.friendFundingPageItemDtoList)
        ? friendFundingData.friendFundingPageItemDtoList
        : [];

    return (
        <div className="friendFunding-itemImg">
            <div className="frienditemlist">
                <div className="friendFunding-item-itemimg">
                    <img className="fundingItem1" alt="Rectangle"
                         src={toImageProxyUrl(fundingItems[0]?.itemImageUrl || NonItemImg)}/>

                    <div className="fundingItemSub">
                        {/*펀딩 item 존재 여부 체크*/}
                        {fundingItems.length > 1 ? (
                            <img className="fundingItem2" alt="Rectangle"
                                 src={toImageProxyUrl(fundingItems[1]?.itemImageUrl || NonItemImg)}/>
                        ) : (
                            <img className="fundingItem2" alt="Rectangle" src={NonItemImg}/>
                        )}

                        {fundingItems.length > 2 ? (
                            <img className="fundingItem3" alt="Rectangle"
                                 src={toImageProxyUrl(fundingItems[2]?.itemImageUrl || NonItemImg)}/>
                        ) : (
                            <img className="fundingItem3" alt="Rectangle" src={NonItemImg}/>
                        )}
                    </div>
                </div>


            </div>
        </div>
    );
};

export default FriendFundingItemImg;
