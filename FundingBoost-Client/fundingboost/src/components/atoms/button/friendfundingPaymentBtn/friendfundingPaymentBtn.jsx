import React from 'react';
import Button from 'react-bootstrap/Button';
import '../yellowBtn.scss';
import axios from 'axios';


function FriendFundingPaymentBtn({ fundingId, barcodeToken, disabled }) {
    const handlePayment = async () =>{
        try{
            if (!barcodeToken) {
                alert('바코드 토큰이 준비되지 않았습니다. 잠시 후 다시 시도해주세요.');
                return;
            }
            const accessToken = localStorage.getItem('accessToken');

            await axios.post(`${process.env.REACT_APP_FUNDINGBOOST}/pay/friends/${fundingId}/barcode-token/consume`, {
                token: barcodeToken
            }, {

                responseType: 'json',
                headers: ({
                    "Content-Type" : "application/json",
                    "Authorization": `Bearer ${accessToken}`,
                }),
                withCredentials: true,
            });
            window.location.href = '/funding/pay/success';
        }catch(error){
            console.error('POST 에러:', error);
        }
    }


    return (
        <div className="friendToFundingBtn-btn">
            <Button className="yellowBtn" onClick={handlePayment} disabled={disabled}>결제하기</Button>
        </div>
    );
}

export default FriendFundingPaymentBtn;
