import React from 'react';
import './friendFundingDetail-item.scss';
import NonItemImg from '../../../../assets/nonItemImg.svg';
import { toImageProxyUrl } from "../../../../utils/imageProxyUrl";

const FriendFundingDetailItem = ({ friendFundingDetailData }) => {
    const itemList = friendFundingDetailData?.data?.friendFundingItemList ?? [];

    return (
        <div className="friendFundingDetailItem">
            <div className="friendFundingDetailItem-headerCard">
                <div className="friendFundingDetailItem-heading">펀딩 상품</div>
                <div className="friendFundingDetailItem-subcopy">현재 참여 가능한 상품을 확인하고 바로 펀딩할 수 있습니다.</div>
            </div>
            <div className="friendFundingDetailItem-list">
                {itemList.map((item) => (
                    <div key={`${item.itemId}-${item.optionName}`} className="friendFundingDetailItem-itemContainer">
                        <img src={toImageProxyUrl(item.itemImageUrl || NonItemImg)} alt={item.itemName} className="item-img" />
                        <div className="friendFundingDetailItem-itemDetail">
                            <div className="friendFundingDetailItem-itemtitle">{item.itemName}</div>
                            <div className="friendFundingDetailItem-optionDetail">
                                <div className="friendFundingDetailItem-optionGroup">
                                    <div className="friendFundingDetailItem-option">옵션</div>
                                </div>
                                <div className="friendFundingDetailItem-optionName">{item.optionName || '기본 옵션'}</div>
                            </div>
                            <div className="friendFundingDetailItem-price">{Number(item.itemPrice).toLocaleString()} 원</div>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
};

export default FriendFundingDetailItem;
