import React from 'react';
import './orderproductdetail.scss';
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

const OrderProductDetail = ({ selectedItems }) => {

    const item = selectedItems;
    console.log("Item:", item);
    const safeOptionName = item.optionName || "기본 옵션";
    const quantity = item.quantity || 1;
    const lineTotalPrice = item.itemPrice * quantity;

    return (
        <div className="order-padding-area">
                <div className="MyPayItemOverlap">
                    <div className="MyPayItem-left-group">
                        <img className="MyPayItem-image" alt="ItemImage" src={toImageProxyUrl(item.itemImageUrl)}/>
                        <div className="name-option-group">
                            <p className="MyPayItem-item-name">{item.itemName}</p>
                            <div className="MyPayItem-option-group">
                                <div className="MyPayItem-option-box">
                                    <div className="MyPayItem-option">옵션</div>
                                </div>
                                <div className="MyPayItem-option-name">{safeOptionName}</div>
                            </div>
                            <div className="quantity-wrapper">
                            <div className="MyPayItem-quantity-wrapper">[ 수량 ]</div>
                                <div className="MyPayItem-quantity"> {quantity}</div>
                            </div>
                        </div>
                    </div>
                    <div className="MyPayItem-price">{lineTotalPrice.toLocaleString()} 원</div>
                </div>
        </div>
    );
};

export default OrderProductDetail;
