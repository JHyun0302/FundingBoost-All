import React, { useEffect, useMemo, useRef, useState } from "react";
import "./orderpaypayment.scss";
import axios from "axios";
import { useNavigate } from "react-router-dom";
import { createIdempotencyKey } from "../../../utils/idempotencyKey";

export default function OrderpayPoint({ point, selectedItems, onUpdateUsingPoint, totalPrice, selectedDeliveryItem }) {
    const PAYMENT_METHOD = {
        MIXED: "MIXED",
        POINT_ONLY: "POINT_ONLY"
    };
    const [inputAmount, setInputAmount] = useState("0");
    const [paymentMethod, setPaymentMethod] = useState(PAYMENT_METHOD.MIXED);
    const idempotencyKeyRef = useRef(createIdempotencyKey());
    const navigate = useNavigate();

    const numericPoint = Math.max(Number(point) || 0, 0);
    const numericTotalPrice = Math.max(Number(totalPrice) || 0, 0);
    const maxUsablePoint = Math.min(numericPoint, numericTotalPrice);
    const parsedInputAmount = useMemo(() => {
        const amount = Number(String(inputAmount).replaceAll(",", ""));
        if (!Number.isFinite(amount)) {
            return 0;
        }
        return Math.max(amount, 0);
    }, [inputAmount]);
    const usingPoint = Math.min(parsedInputAmount, maxUsablePoint);
    const directPayAmount = Math.max(numericTotalPrice - usingPoint, 0);
    const pointOnlyInsufficient = paymentMethod === PAYMENT_METHOD.POINT_ONLY && directPayAmount > 0;

    useEffect(() => {
        if (parsedInputAmount > maxUsablePoint) {
            setInputAmount(maxUsablePoint.toString());
        }
    }, [parsedInputAmount, maxUsablePoint]);

    useEffect(() => {
        onUpdateUsingPoint(usingPoint);
    }, [onUpdateUsingPoint, usingPoint]);

    const handleOrderPayment = async () => {
        if (!selectedDeliveryItem) {
            alert("주소를 선택해주세요.");
            return;
        }
        if (pointOnlyInsufficient) {
            alert("포인트가 부족합니다. '포인트+일반결제'를 선택해주세요.");
            return;
        }

        if (paymentMethod === PAYMENT_METHOD.MIXED && directPayAmount > 0) {
            const confirmed = window.confirm(
                `포인트 ${usingPoint.toLocaleString()}P를 사용하고 부족한 ${directPayAmount.toLocaleString()}원은 일반결제로 진행할까요?`
            );
            if (!confirmed) {
                return;
            }
        }

        try {
            const accessToken = localStorage.getItem('accessToken');

            const itemPayDtoList = Array.isArray(selectedItems) ? selectedItems.map(item => ({
                itemId: item.itemId,
                giftHubId: item.giftHubItemId,
                quantity: item.quantity,
                optionName: item.optionName
            })) : [];

            const data = {
                itemPayDtoList,
                deliveryId: selectedDeliveryItem?.deliveryId,
                usingPoint
            };

            const datanow = itemPayDtoList.length > 0 ? {
                itemId: itemPayDtoList[0].itemId,
                quantity:itemPayDtoList[0].quantity,
                deliveryId: selectedDeliveryItem?.deliveryId,
                usingPoint,
                optionName: itemPayDtoList[0].optionName
            } : {};

            const url = itemPayDtoList.some(item => item.giftHubId === null || item.giftHubId === undefined)
                ? `${process.env.REACT_APP_FUNDINGBOOST}/pay/order/now`
                : `${process.env.REACT_APP_FUNDINGBOOST}/pay/order`;

            const postData = itemPayDtoList.some(item => item.giftHubId === null || item.giftHubId === undefined)
                ? datanow
                : data;

            const response = await axios.post(url, postData, {
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
            navigate("/order/pay/success");
        } catch (error) {
            const errorCode = error?.response?.data?.code;
            if (errorCode === 50000) {
                navigate("/error");
                return;
            }
            alert(error?.response?.data?.message || "결제에 실패했습니다. 다시 시도해주세요.");
        }
    };

    const handleInputChange = (event) => {
        const onlyDigits = event.target.value.replace(/[^\d]/g, "");
        if (!onlyDigits) {
            setInputAmount("0");
            return;
        }
        const value = Number(onlyDigits);
        if (!Number.isFinite(value)) {
            setInputAmount("0");
            return;
        }
        setInputAmount(Math.min(value, maxUsablePoint).toString());
    };

    const handleUseAllPoints = () => {
        setInputAmount(maxUsablePoint.toString());
    };

    return (
        <div className="MyPayPointBox">
            <div className="MyPayPointView">
                <div className="MyPayPointTitle">포인트 사용</div>
                <div className="MyPayPointGroup">
                    <div className="MyPayPointInput-wrapper">
                        <div className="MyPayPointInput">
                            <input
                                type="text"
                                className="point"
                                placeholder="0"
                                value={usingPoint.toLocaleString()}
                                onChange={handleInputChange}
                            />
                        </div>
                    </div>
                    <div className="MyPayPointText-P">P</div>
                    <button className="MyPayPointUse-all-point" onClick={handleUseAllPoints}>전액사용</button>
                </div>
                <p className="MyPayPoint-remain-point">사용 가능 포인트 {numericPoint.toLocaleString()} P</p>
            </div>

            <div className="MyPayMethodBox">
                <div className="MyPayMethodTitle">결제 수단</div>
                <div className="MyPayMethodOptions">
                    <button
                        type="button"
                        className={`MyPayMethodOption ${paymentMethod === PAYMENT_METHOD.MIXED ? "active" : ""}`}
                        onClick={() => setPaymentMethod(PAYMENT_METHOD.MIXED)}
                    >
                        포인트 + 일반결제
                        <span className="methodBadge">추천</span>
                    </button>
                    <button
                        type="button"
                        className={`MyPayMethodOption ${paymentMethod === PAYMENT_METHOD.POINT_ONLY ? "active" : ""}`}
                        onClick={() => setPaymentMethod(PAYMENT_METHOD.POINT_ONLY)}
                    >
                        포인트만 결제
                    </button>
                </div>
                {pointOnlyInsufficient && (
                    <p className="MyPayMethodWarning">
                        현재 포인트로는 전액 결제가 불가능합니다. 일반결제를 함께 선택해주세요.
                    </p>
                )}
            </div>

            <div className="MyPayFundingPaymentInformationBox">
                <div className="MyPayFundingPaymentInformationView">
                    <div className="MyPayFundingPaymentInformationText">결제정보</div>
                    <div className="line" alt="Line"/>
                    <div className="MyPayFundingPaymentInformationTotal-group">
                        <div className="MyPayFundingPaymentInformationGroup">
                            <div className="MyPayFundingPaymentInformationText">총 상품 금액</div>
                            <div className="text-price">{numericTotalPrice.toLocaleString()} 원</div>
                        </div>
                        <div className="MyPayFundingPaymentInformationGroup">
                            <div className="MyPayFundingPaymentInformationText">사용한 포인트</div>
                            <div className="MyPayFundingPaymentInformationOverlap-group">
                                <div className="text-price">- {usingPoint.toLocaleString()} 원</div>
                            </div>
                        </div>
                        <div className="MyPayFundingPaymentInformationGroup">
                            <div className="MyPayFundingPaymentInformationText">일반 결제 예정</div>
                            <div className="text-price">{directPayAmount.toLocaleString()} 원</div>
                        </div>
                    </div>
                    <div className="line" alt="Line"/>
                    <div className="total-price">{directPayAmount.toLocaleString()} 원</div>
                </div>
                <button
                    className="pay-request-button"
                    disabled={pointOnlyInsufficient || numericTotalPrice <= 0}
                    onClick={handleOrderPayment}
                >
                    {pointOnlyInsufficient
                        ? "포인트가 부족합니다"
                        : `${directPayAmount.toLocaleString()}원 결제하고 주문하기`}
                </button>
            </div>
        </div>
    );
};
