//펀딩 바로가기 버튼
import React from 'react';
import Button from 'react-bootstrap/Button';
import { useNavigate } from 'react-router-dom';
import  '../yellowBtn.scss';

function FundingRegistButton({ onClick, tagIsSelected, orderedItems }){
    const navigate = useNavigate();
    const handleClick = () => {
        if (tagIsSelected && orderedItems.every(item => item.order !== null && item.order !== undefined)) {
            onClick();
            navigate('/funding/regist/success');
        } else {
            if (!tagIsSelected) {
                alert('태그를 선택해주세요!');
            } else {
                alert('상품 순서를 모두 지정해주세요!');
            }
        }
    };
    return (
        <>
            <div className="FundingRegist-btn">
                <Button className="yellowBtn" onClick={ handleClick }>펀딩 등록하기</Button>

            </div>
        </>

    );
}
export default FundingRegistButton;