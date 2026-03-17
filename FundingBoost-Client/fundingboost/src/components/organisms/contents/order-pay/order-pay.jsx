import React, { useState, useEffect } from 'react';
import './order-pay.scss';
import axios from 'axios';
import { useLocation } from 'react-router-dom';
import OrderProductDetail from "../../../atoms/OrderProductDetail/orderproductdetail";
import OrderpayPoint from "../../../atoms/OrderPayPayment/orderpaypayment";
import OrderPayDelivery from "../../../atoms/OrderPayDelivery/orderpaydelivery";
import NonMemberModal from "../../../atoms/nonMemberModal/nonMemberModal";

const OrderPane = () => {
    const [apiData, setApiData] = useState(null);
    const [fundingItemData, setFundingItemData] = useState(null);
    const [deliveryDtoList, setDeliveryDtoList] = useState([]);
    const location = useLocation();
    const [point, setPoint] = useState(null);
    const [modalShowState, setModalShowState] = useState(false);
    const [selectedDeliveryItem, setSelectedDeliveryItem] = useState(null);

    const { selectedItems, itemPurchase } = location.state || {};
    const [purchaseItem, setPurchaseItem] = useState(itemPurchase ? [itemPurchase] : selectedItems || []);

    console.log("selectedItems",purchaseItem);

    console.log(deliveryDtoList);
    console.log(purchaseItem);

    useEffect(() => {
        const fetchOrderPayData = async () => {
            try {
                const accessToken = localStorage.getItem('accessToken');
                if (!accessToken) {
                    setModalShowState(true);
                    return;
                }

                const response = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/pay/view/order`, {
                    responseType: 'json',
                    headers: {
                        'Content-Type': 'application/json',
                        "Authorization": `Bearer ${accessToken}`,
                    }
                });
                console.log('GET 결과:', response.data);
                setFundingItemData(response.data);
                setApiData(response.data.data);
                setDeliveryDtoList(response.data.data.deliveryDtoList);
                setPoint(response.data.data.point);
            } catch (error) {
                console.error('GET 에러:', error);
            }
        };

        fetchOrderPayData();
    }, []);

    const onUpdateUsingPoint = (value) => {
        console.log("사용 포인트가 업데이트되었습니다:", value);
    };

    const getTotalPrice = () => {
        if (purchaseItem.length > 0) {
            const totalPrice = purchaseItem.reduce((accumulator, currentItem) => {
                return accumulator + (currentItem.itemPrice * currentItem.quantity);
            }, 0);
            return totalPrice;
        }
        return 0;
    };

    return (
        <div className="order-pay-page-container">
            {modalShowState && <NonMemberModal message="로그인 후 펀딩부스트를 시작해보세요." />}
            <div className="orderpay-left-container">
                <div className="mypay-product-details-text">상품내역</div>
                <div className="MyOrderItemBox">
                    {purchaseItem && purchaseItem.map((item, index) => (
                        <OrderProductDetail key={index} selectedItems={item} />
                    ))}
                </div>
                <OrderPayDelivery
                    deliveryDtoList={deliveryDtoList}
                    onSelectItem={setSelectedDeliveryItem} // 선택한 배송지 설정
                />
            </div>
            <div className="orderpay-right-container">
                <div className="orderpayment-container">
                    {purchaseItem.length > 0 && (
                        <OrderpayPoint
                            selectedItems={purchaseItem}
                            totalPrice={getTotalPrice()}
                            point={point}
                            onUpdateUsingPoint={onUpdateUsingPoint}
                            selectedDeliveryItem={selectedDeliveryItem} // 선택한 배송지 정보 전달
                        />
                    )}
                </div>
            </div>
        </div>
    );
}

export default OrderPane;
