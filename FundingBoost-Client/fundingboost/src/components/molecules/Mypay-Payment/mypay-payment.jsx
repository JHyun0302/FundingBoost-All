import React, { useRef, useState } from "react";
import "./mypay-payment.scss";
import axios from "axios";
import {useNavigate} from "react-router-dom";
import { createIdempotencyKey } from "../../../utils/idempotencyKey";

export default function MyPayPoint ({collectPrice, point, onUpdateUsingPoint, selectedItemDto, selectedDeliveryItem }) {
    const itemPrice = selectedItemDto.itemPrice;
    const [inputAmount, setInputAmount] = useState("");
    const [usingPoint, setUsingPoint] = useState(0); // usingPoint 상태 추가
    const idempotencyKeyRef = useRef(createIdempotencyKey());
    const navigate = useNavigate();
    const discountPrice = (itemPrice * selectedItemDto.itemPercent) / 100; //펀딩된 금액
    console.log(discountPrice);

    const userMaxPoint=()=>{
        const maxAvailablePoints = itemPrice - discountPrice;
        console.log("사용가능 포인트:",maxAvailablePoints);
        return Math.min(maxAvailablePoints, point); // 사용할 수 있는 최대 포인트는 포인트와 상품 가격 중 작은 값
    }

    const handleUseAllPoints = () => {
        const maxPoint = userMaxPoint();
        setInputAmount(maxPoint);
        setUsingPoint(maxPoint); // usingPoint 업데이트
        onUpdateUsingPoint(maxPoint); // 부모 컴포넌트로 usingPoint 업데이트
    };

    const handleInputChange = (event) => {
        const value = Number(event.target.value);
        const maxPoint = userMaxPoint();

        if (value > maxPoint) {
            setInputAmount(maxPoint.toString());
        } else if (value < 0) {
            setInputAmount("0");
        } else {
            setInputAmount(value.toString());
        }

        setUsingPoint(value || 0);
        onUpdateUsingPoint(value || 0);
    };

    const handlePayment = async () => {
        try {
            if(!selectedDeliveryItem){
                alert("배송지를 선택해주세요!");
                return;
            }
            const accessToken = localStorage.getItem('accessToken');

            const postData = {
                usingPoint: usingPoint,
                deliveryId: selectedDeliveryItem?.deliveryId
            };
            const fundingItemId = selectedItemDto.fundingItemId;
            const response = await axios.post(`${process.env.REACT_APP_FUNDINGBOOST}/pay/funding/${fundingItemId}`, postData, {
                responseType: 'json',
                headers: {
                    'Content-Type': 'application/json',
                    "Authorization": `Bearer ${accessToken}`,
                    "Idempotency-Key": idempotencyKeyRef.current
                }
            });
            if (response.data.success === false) {
                // 에러 코드가 50000인 경우 에러 페이지로 리다이렉션
                if (response.data.error && response.data.error.code === 50000) {
                    navigate("/error");
                    return;
                }
            }

            console.log('POST 결과:', response.data);
            navigate("/order/pay/success");
            // 성공 시 처리
        } catch (error) {
            console.error('POST 에러:', error);
            // 실패 시 처리
        }
    };

    const calculateTotalPrice = () => {
        // null 체크 및 기본값 설정
        const safeItemPrice = itemPrice || 0;
        const safeInputAmount = isNaN(parseFloat(inputAmount)) ? 0 : parseFloat(inputAmount);

        // 총 가격 계산 로직에서 null이 아닌 값을 사용
        const remainingPrice = safeItemPrice - safeInputAmount - discountPrice;
        return remainingPrice > 0 ? remainingPrice.toLocaleString() : "0";
    };

    const displayedCollectPrice = collectPrice >= itemPrice ? itemPrice : collectPrice;
    console.log(selectedItemDto.itemPercent);
    const payTotalPrice = calculateTotalPrice(); // 변수명 변경
    const MyPoint = Number(point)
    return (
        <div className="MyPayPointBox">
            <div className="MyPayPointView">
                <div className="MyPayPointTitle">포인트</div>
                <div className="MyPayPointGroup">
                    <div className="MyPayPointInput-wrapper">
                        <div className="MyPayPointInput">
                            <input
                                type="text"
                                className="point"
                                placeholder="0"
                                value={inputAmount} // 입력된 금액 표시
                                onChange={handleInputChange} // 입력값 변경 시 핸들러 호출
                            />
                        </div>
                    </div>
                    <div className="MyPayPointText-P">P</div>
                    <div className="MyPayPointButton-group-wrapper">
                        <div className="MyPayPointButton-group">
                            <button className="MyPayPointUse-all-point" onClick={handleUseAllPoints}>전액사용</button>
                        </div>
                    </div>
                </div>
                <p className="MyPayPoint-remain-point">사용 가능 포인트 {MyPoint.toLocaleString()} P</p>
            </div>

            <div className="MyPayFundingPaymentInformationBox">
                <div className="MyPayFundingPaymentInformationView">
                    <div className="MyPayFundingPaymentInformationText">결제정보</div>
                    <div className="line" alt="Line"/>
                    <div className="MyPayFundingPaymentInformationTotal-group">
                        <div className="MyPayFundingPaymentInformationGroup">
                            <div className="MyPayFundingPaymentInformationText">총 상품 금액</div>
                            <div className="text-price">{itemPrice ? itemPrice.toLocaleString() : 0} 원</div>
                        </div>
                        <div className="MyPayFundingPaymentInformationGroup">
                            <div className="MyPayFundingPaymentInformationText">사용한 포인트</div>
                            <div className="MyPayFundingPaymentInformationOverlap-group">
                                <div className="text-price">- {inputAmount.toLocaleString() || "0"} 원</div> {/* 입력된 금액을 표시 */}
                            </div>
                        </div>
                        <div className="MyPayFundingPaymentInformationGroup">
                            <div className="MyPayFundingPaymentInformationText">펀딩 된 금액</div>
                            <div className="MyPayFundingPaymentInform
                            ationOverlap-group">
                                <div
                                    className="text-price">- {discountPrice ? discountPrice.toLocaleString() : 0} 원
                                </div>
                            </div>
                        </div>
                    </div>
                    <div className="line" alt="Line"/>
                    <div className="total-price">{payTotalPrice.toLocaleString()} 원</div>
                    {/* 총 가격 표시 */}
                </div>
                <button className="pay-request-button" onClick={handlePayment}> 결제하기</button>
            </div>
        </div>
    );
};
