import React from 'react';
import './orderHistory-singleOrder.scss';
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";
const OrderHistorySingleOrder = ({orderHistoryData, onOpenDetail}) => {
    const createdDate = () =>
        (Date().now() + orderHistoryData.createdDate).toISOString().split('T')[0];

    return (
        <div className="orderHistorySingleOrder">
            <button
                type="button"
                className="MyPageOrderHistoryOneView"
                onClick={() => onOpenDetail(orderHistoryData.orderItemId)}
            >
                <div className="MyPageOrderHistoryOneGroup">
                    <img className="MyPageOrderHistoryOneEllipse" alt="Ellipse" src={toImageProxyUrl(orderHistoryData.itemImageUrl)}/>
                    <div className="MyPageOrderHistoryOneOverlap">
                        <div className="MyPageOrderHistoryOneText-wrapper-6">{orderHistoryData.createdDate
                        }</div>
                        <p className="MyPageOrderHistoryOne-p">{orderHistoryData.itemName}</p>
                        <div className="MyPageOrderHistoryOneOptionQuantity">
                            <div className="MyPageOrderHistoryOneOption-group">
                                <div className="MyPageOrderHistoryOneOptionMark">
                                    <div className="MyPageOrderHistoryOneOverlap-group">
                                        <div className="MyPageOrderHistoryOneText-wrapper-2">옵션</div>
                                    </div>
                                </div>
                                <div className="MyPageOrderHistoryOneText-wrapper-3">{orderHistoryData.optionName}</div>
                            </div>
                            <div className="MyPageOrderHistoryOneOption-group">
                                <div className="MyPageOrderHistoryOneOverlap-wrapper">
                                    <div className="MyPageOrderHistoryOneOverlap-group">
                                        <div className="MyPageOrderHistoryOneText-wrapper-4">수량</div>
                                    </div>
                                </div>
                                <div className="MyPageOrderHistoryOneText-wrapper-5">{orderHistoryData.quantity}</div>
                            </div>
                        </div>
                    </div>

                </div>
                <div className="MyPageOrderHistoryOneText-wrapper">{orderHistoryData.price.toLocaleString()} 원</div>
            </button>
        </div>
    );
};

export default OrderHistorySingleOrder;
