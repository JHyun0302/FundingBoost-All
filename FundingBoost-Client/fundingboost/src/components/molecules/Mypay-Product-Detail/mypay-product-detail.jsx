import React from 'react';
import './mypay-product-detail.scss';
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

const MypayProductDetail = ({ selectedItemDto }) => {

    console.log("Item:", selectedItemDto);

        const item = selectedItemDto;

        return (
            <div>
                <div className="mypay-product-details-text">상품내역</div>
                <div className="MyPayItemBox">
                    <div className="MyPayItemOverlap">
                        <div className="MyPayItem-left-group">
                            <img className="MyPayItem-image" alt="ItemImage" src={toImageProxyUrl(item.itemImageUrl)}/>
                            <div className="name-option-group">
                                <p className="MyPayItem-item-name">{item.itemName}</p>
                                <div className="MyPayItem-option-group">
                                    <div className="MyPayItem-option-box">
                                        <div className="MyPayItem-option">옵션</div>
                                    </div>
                                    <div className="MyPayItem-option-name">{item.optionName}</div>
                                </div>
                            </div>
                        </div>
                        <div className="MyPayItem-price">{item.itemPrice.toLocaleString()} 원</div>
                    </div>
                </div>
            </div>
        );
};

export default MypayProductDetail;
