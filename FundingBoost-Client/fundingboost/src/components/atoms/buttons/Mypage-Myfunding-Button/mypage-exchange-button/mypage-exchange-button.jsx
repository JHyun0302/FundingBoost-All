import React, { useState } from "react";
import './mypage-exchange-button.scss';
import axios from "axios";
import ExchangePoint from "../../../../molecules/Modal/CheckExchangePoint/checkexchangepoint";

const MypageExchangeBtn = ({ item, memberId }) => {
    const [refresh, setRefresh] = useState(false);

    const handleExchange = async () => {
        // 요청 보내는 부분은 ExchangePoint 컴포넌트 내부로 이동합니다.
    };

    if (refresh) {
        window.location.reload(); // 페이지 새로고침
    }

    return (
        <ExchangePoint item={item} handleExchange={handleExchange} />
    );
}

export default MypageExchangeBtn;
