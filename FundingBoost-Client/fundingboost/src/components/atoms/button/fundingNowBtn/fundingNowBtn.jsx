import React from 'react';
import Button from "react-bootstrap/Button";
import './fundingNowBtn.scss'
import {useNavigate} from "react-router-dom";


function FundingNowBtn({selectOption, itemId, itemName, itemPrice, itemThumbnailImageUrl, quantity, onNonMemberModalOpen}) {
    const navigate = useNavigate();

    console.log(quantity);
    const fundingNowBtnClick = () => {
        const accessToken = localStorage.getItem('accessToken');

        if (!accessToken) {
            onNonMemberModalOpen();
            return;
        }

        if(quantity>5){
            alert('펀딩은 최대 5개까지만 가능합니다. ' +
                '수량을 확인해주세요');
         return;
        }
        if(selectOption&&selectOption !=="상품 옵션을 선택해주세요."){
            const fundingNowData= {
                optionName : selectOption,
                itemName : itemName,
                itemPrice : itemPrice,
                itemImageUrl : itemThumbnailImageUrl,
                itemId:itemId,
                quantity : quantity
            };
            localStorage.setItem("fundingReturnItemId", String(itemId));
            navigate(`/funding?itemId=${itemId}`, { state: { fundingNowData } });
        }else{
            alert('상품 옵션을 선택해주세요');
        }

    };

    return (
        <div className="fundingNowBtn">
            <div className="fundingBox">
                <Button className="fundingBtn" onClick={fundingNowBtnClick}>바로 펀딩하기</Button>
            </div>
        </div>
    );
}

export default FundingNowBtn;
