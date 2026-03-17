 import React from "react";
import { useState } from "react";
import './mypage-myfunding-extension-button.scss';
import axios from "axios";


const ExtensionButton = ({ myPageFundingItemDtoList }) => {
    const [refresh, setRefresh] = useState(false);
    const handleExtensionButton = async () => {
        const fundingId = myPageFundingItemDtoList[0]?.fundingId;
        const memberId = myPageFundingItemDtoList[0]?.memberId;
        const requestData = { fundingId, memberId };


        try {
            const accessToken = localStorage.getItem('accessToken');

            const response = await axios.post(`${process.env.REACT_APP_FUNDINGBOOST}/funding/extension/${fundingId}`, requestData, {

                responseType: 'json',
                headers: {
                    'Content-Type': 'application/json',
                    "Authorization": `Bearer ${accessToken}`,
                },
                withCredentials: true
            });
            console.log('POST 결과:', response.data);
            setRefresh(true); // 상태 변경
        } catch (error) {
            console.error('POST 에러:', error);
        }
    }

    if (refresh) {
        window.location.reload(); // 페이지 새로고침
    }

    return (
        <button className="mypage-extension-btn" onClick={handleExtensionButton}>기간 연장하기</button>
    );
}

export default ExtensionButton;