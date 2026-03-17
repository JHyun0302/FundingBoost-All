import React , { useState }from "react";
import "./pointUse.scss";
import {useLocation, useParams} from 'react-router-dom';


export default function FriendFundingMyPoint({friendFundingPayData, fundingAmount, onUpdatePoints}) {
    const [usePoints, setUsePoints] = useState("0");

    console.log("Funding: "+fundingAmount);
    if (!friendFundingPayData) {
        return 0;
    }

    //전액 사용 버튼 사용
    const handleUseAllPoints = () => {
        let allPoints = friendFundingPayData.myPoint;
        if (allPoints > fundingAmount) {
            allPoints = fundingAmount;
        }
        setUsePoints(allPoints);
        onUpdatePoints(allPoints);
    };

    //텍스트로 입력하여 포인트 사용
    const useInputPoints = (e) =>{
        let inputPoints = e.target.value;
        //문자 입력 제한
        inputPoints = inputPoints.replace(/\D/g, '');
        // inputPoints = parseInt(inputPoints);
        if (isNaN(inputPoints)) return;
        // 입력 데이터 전액 보다 클 경우 전액으로 변경
        if (inputPoints > friendFundingPayData.myPoint) {
            inputPoints = friendFundingPayData.myPoint;
        }
        if (inputPoints > fundingAmount) {
            inputPoints = fundingAmount;
        }
        setUsePoints(inputPoints);
        onUpdatePoints(inputPoints);
    }

    //천단위 ,
    const locleUsePoints = usePoints.toLocaleString();

    return (
        <div className="my-point">
            <div className="my-point-first-row">
                <div className="my-point-fixed-text" >포인트</div>
                <input className="my-point-input"
                       type="text"
                       value={locleUsePoints}
                       onChange={useInputPoints }/>
                <p className="my-point-unit">P</p>
                <button className="my-point-use-all-point"  onClick={handleUseAllPoints}>전액사용</button>
            </div>
            <p className="my-point-use-usable-point">사용 가능 포인트 {friendFundingPayData.myPoint.toLocaleString()} P</p>
        </div>
    );
};