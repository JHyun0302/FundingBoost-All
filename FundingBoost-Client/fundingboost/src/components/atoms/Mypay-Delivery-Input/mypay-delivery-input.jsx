import React, { useEffect, useState } from "react";
import "./mypay-delivery-input.scss";
import { formatPhoneNumber } from "../../../utils/formatPhoneNumber";

export default function MyPayDeliveryInput ({ selectedItem }) {
    const [selectedDelivery, setSelectedDelivery] = useState({
        customerName: "",
        postalCode: "",
        address: "",
        phoneNumber: "",
        deliveryMemo: "",
    });

    useEffect(() => {
        if (selectedItem) {
            setSelectedDelivery({
                customerName: selectedItem.customerName || "",
                postalCode: selectedItem.postalCode || "",
                address: selectedItem.address || "",
                phoneNumber: formatPhoneNumber(selectedItem.phoneNumber || ""),
                deliveryMemo: selectedItem.deliveryMemo || "",
            });
        }
    }, [selectedItem]);

    return (
        <div className="MyPayDeliveryInputBox">
            <div className="MyPayDeliveryInputView">
                <div className="MyPayDeliveryInputHint">
                    저장된 배송지를 선택하면 아래 정보가 자동으로 채워집니다.
                </div>

                <div className="MyPayDeliveryField">
                    <div className="MyPayDeliveryInputText">수령인</div>
                    <input
                        type="text"
                        className="MyPayDeliveryInputRectangle"
                        placeholder="수령인을 선택해주세요"
                        value={selectedDelivery.customerName}
                        readOnly
                    />
                </div>

                <div className="MyPayDeliveryRow">
                    <div className="MyPayDeliveryField">
                        <div className="MyPayDeliveryInputText">우편번호</div>
                        <input
                            type="text"
                            className="MyPayDeliveryInputRectangle"
                            placeholder="우편번호"
                            value={selectedDelivery.postalCode}
                            readOnly
                        />
                    </div>
                    <div className="MyPayDeliveryField">
                        <div className="MyPayDeliveryInputText">연락처</div>
                        <input
                            type="text"
                            className="MyPayDeliveryInputRectangle"
                            placeholder="연락처"
                            value={selectedDelivery.phoneNumber}
                            readOnly
                        />
                    </div>
                </div>

                <div className="MyPayDeliveryField">
                    <div className="MyPayDeliveryInputText">배송지 주소</div>
                    <input
                        type="text"
                        className="MyPayDeliveryInputRectangle"
                        placeholder="배송지 주소"
                        value={selectedDelivery.address}
                        readOnly
                    />
                </div>

                <div className="MyPayDeliveryField">
                    <div className="MyPayDeliveryInputText">배송 메모</div>
                    <textarea
                        className="MyPayDeliveryInputRectangle-2"
                        placeholder="배송 메모가 없습니다."
                        value={selectedDelivery.deliveryMemo}
                        readOnly
                    />
                </div>
            </div>
        </div>
    );
};
