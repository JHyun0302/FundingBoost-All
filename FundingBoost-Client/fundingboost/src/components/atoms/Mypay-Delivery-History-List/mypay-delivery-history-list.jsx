import React from "react";
import "./mypay-delivery-history-list.scss";
import MypayDeliveryHistory from "../Mypay-Delivery-History/mypay-delivery-history";

export default function MypayDeliveryHistoryList({ deliveryDtoList, onSelectItem }) {
    if (!deliveryDtoList || deliveryDtoList.length === 0) {
        return <div>No delivery history</div>;
    }

    return (
        <div className="MyPayDeliveryListBox">
            {deliveryDtoList.map((delivery, index) => (
                <div key={index} className="MyPayDeliveryListGroup">
                    <div className="MyPayDeliveryListView-wrapper">
                        <MypayDeliveryHistory key= {index} delivery={delivery} onSelectItem={onSelectItem}/>
                    </div>
                </div>
            ))}
        </div>
    );
};

