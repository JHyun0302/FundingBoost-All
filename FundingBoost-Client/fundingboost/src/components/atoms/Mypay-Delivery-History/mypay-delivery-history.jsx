import React from "react";
import "./mypay-delivery-history.scss";

export default function MypayDeliveryHistory ({delivery, onSelectItem, isSelected}) {
    const { customerName, address, phoneNumber, postalCode, deliveryMemo } = delivery;

    return (
        <div
            className={`MyPayDeliveryBox ${isSelected ? "selected" : ""}`}
            role="button"
            tabIndex={0}
            onClick={() => onSelectItem(delivery)}
            onKeyDown={(event) => {
                if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    onSelectItem(delivery);
                }
            }}
        >
            <div className="MyPayDeliveryView">
                <div className="MyPayDeliveryName">{customerName}</div>
                <div className="MyPayDeliveryPostalCode">
                    우편번호 {postalCode || "미등록"}
                </div>
                <p className="MyPayDeliveryAddress">{address}</p>
                <div className="MyPayDeliveryPhoneNumber">{phoneNumber}</div>
                <div className="MyPayDeliveryMemo">
                    메모: {deliveryMemo || "배송 메모가 없습니다."}
                </div>
            </div>
        </div>
    );
};
