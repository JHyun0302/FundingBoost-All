import React from "react";
import './mypage-delivery-button.scss';
import {useNavigate} from "react-router-dom";

function MypageDeliveryBtn({selectedItemDto}) {

    const navigate = useNavigate();

    const handleDeliveryButtonClick = () => {
        navigate('/funding/pay', { state: {selectedItemDto}});
        console.log(selectedItemDto)
    };

    return (
        <button className="mypage-delivery-btn" onClick={handleDeliveryButtonClick} >배송지 입력하기</button>
    );
}

export default MypageDeliveryBtn;