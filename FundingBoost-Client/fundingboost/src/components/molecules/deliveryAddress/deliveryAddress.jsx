import React from "react";
import "./deliveryAddress.scss";

export default function DeliveryAddress({ deliveryData, onDelete }) {
    return (
        <div className="delivery-address">
            <div className="delivery-address-header">
                <div className="delivery-address-name-and-remove">
                    <div className="delivery-address-name">{deliveryData.customerName}</div>
                    <button
                        type="button"
                        className="deliver-address-remove"
                        onClick={() => onDelete(deliveryData.deliveryId)}
                    >
                        삭제
                    </button>
                </div>
                <div className="delivery-address-address-number">
                    {deliveryData.postalCode && (
                        <div className="delivery-address-postal-code">우편번호 {deliveryData.postalCode}</div>
                    )}
                    <p className="delivery-address-address">{deliveryData.address}</p>
                    <div className="deliver-address-phone-number">{deliveryData.phoneNumber}</div>
                    {deliveryData.deliveryMemo && (
                        <div className="delivery-address-memo">메모: {deliveryData.deliveryMemo}</div>
                    )}
                </div>
            </div>
        </div>
    );
};
