import React from "react";
import "./OrderPayDeliveryHistoryList.scss";
import MypayDeliveryHistory from "../Mypay-Delivery-History/mypay-delivery-history";

export default function OrderPayDeliveryHistoryList({ deliveryDtoList, onSelectItem, selectedDeliveryId }) {
    if (!deliveryDtoList || deliveryDtoList.length === 0) {
        return <div className="MyPayDeliveryEmptyState">등록된 배송지가 없습니다.</div>;
    }

    return (
        <div className="MyPayDeliveryListBox">
            {deliveryDtoList.map((delivery) => (
                <div key={delivery.deliveryId} className="MyPayDeliveryListView">
                    <MypayDeliveryHistory
                        delivery={delivery}
                        onSelectItem={onSelectItem}
                        isSelected={selectedDeliveryId === delivery.deliveryId}
                    />
                </div>
            ))}
        </div>
    );
};
