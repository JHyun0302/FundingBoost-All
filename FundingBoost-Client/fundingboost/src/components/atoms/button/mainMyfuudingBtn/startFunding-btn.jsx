//메인페인지 '펀딩 만들기' 버튼
import React from 'react';
import Button from "react-bootstrap/Button";
import  '../yellowBtn.scss';

const StartFundingButton = ({ onClick }) => {
    return (
        <>
            <div className="startFunding-btn">
                <Button href="/gifthub" className="yellowBtn">펀딩 생성하기</Button>{' '}
            </div>
        </>
    );
}

export default StartFundingButton;
