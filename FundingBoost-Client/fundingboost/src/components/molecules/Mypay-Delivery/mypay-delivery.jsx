import React, {useState, useEffect} from 'react';
import './mypay-delivery.scss';
import img from "../../../assets/testitem.svg";
import MyPayDeliveryInput from "../../atoms/Mypay-Delivery-Input/mypay-delivery-input";
import MypayDeliveryHistory from "../../atoms/Mypay-Delivery-History/mypay-delivery-history";
import MypayDeliveryHistoryList from "../../atoms/Mypay-Delivery-History-List/mypay-delivery-history-list";

const MypayDelivery = ({deliveryDtoList, onSelectItem}) => {
    const [selectedItem, setSelectedItem] = useState(null);

    const handleSelectItem = (item) => {
        setSelectedItem(item);
        onSelectItem(item);
    };


    return (
        <div>
            <div className="Mypay-delivery-wrapper">
                <div className="MyPayItemBox01">
                    <div className="mypay-product-details-text01">배송지 입력</div>
                    <MyPayDeliveryInput selectedItem={selectedItem}/>
                </div>
                <div className="MyPayItemBox02">
                    <div className="mypay-product-details-text02">배송지 내역</div>
                    <MypayDeliveryHistoryList deliveryDtoList={deliveryDtoList} onSelectItem={handleSelectItem}/>
                </div>
            </div>

        </div>
    );
};

export default MypayDelivery;
