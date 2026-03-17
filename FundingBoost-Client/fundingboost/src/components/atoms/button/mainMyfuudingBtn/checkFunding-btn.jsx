//펀딩 바로가기 버튼
import React from 'react';
import Button from 'react-bootstrap/Button';
import  '../yellowBtn.scss';

function CheckFundingButton({ onClick }){
    return (
        <>
            <div className="checkFunding-btn">
                <Button href="/mypage" className="yellowBtn">펀딩 바로가기</Button>{' '}
            </div>
        </>

    );
}
export default CheckFundingButton;