import React, { useState, useEffect } from 'react';
import './mypay.scss';
import MypayProductDetail from '../../../molecules/Mypay-Product-Detail/mypay-product-detail';
import MypayDelivery from "../../../molecules/Mypay-Delivery/mypay-delivery";
import MypayPayment from "../../../molecules/Mypay-Payment/mypay-payment";
import axios from 'axios';
import { useLocation } from 'react-router-dom';
import NonMemberModal from "../../../atoms/nonMemberModal/nonMemberModal";

const MypayPane = () => {
    const [apiData, setApiData] = useState(null);
    const [fundingItemData, setFundingItemData] = useState(null);
    const [deliveryDtoList, setdeliveryDtoList] = useState([]); // deliveryDtoList 상태 추가
    const location = useLocation();
    const [collectPrice, setCollectPrice] = useState(null);
    const [point, setPoint] = useState(null);
    const [modalShowState, setModalShowState] = useState(false);
    const [selectedDeliveryItem, setSelectedDeliveryItem] = useState(null);

    const { state: { selectedItemDto } } = location;
    console.log("selectedItemDto ", selectedItemDto);

    useEffect(() => {
        const fetchFundingItemData = async () => {
            try {
                const accessToken = localStorage.getItem('accessToken');
                const fundingItemId = selectedItemDto.fundingItemId;
                console.log(fundingItemId)
                if (!accessToken) {
                    setModalShowState(true);
                    return;
                }

                const response = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/pay/view/funding/${fundingItemId}`, {

                    responseType: 'json',
                    headers: {
                        'Content-Type': 'application/json',
                        "Authorization": `Bearer ${accessToken}`,
                    }
                });
                console.log('GET 결과:', response.data);
                setFundingItemData(response.data);
                setApiData(response.data.data);
                setdeliveryDtoList(response.data.data.deliveryDtoList);
                setCollectPrice(response.data.data.collectPrice);
                setPoint(response.data.data.point);
            } catch (error) {
                console.error('GET 에러:', error);
            }
        };

        fetchFundingItemData();
    }, []);

    const onUpdateUsingPoint = (value) => {
        console.log("사용 포인트가 업데이트되었습니다:", value);
    };

    console.log(onUpdateUsingPoint);

    return(
        <div className="mypay-page-container">
            {modalShowState && <NonMemberModal message="로그인 후 펀딩부스트를 시작해보세요." />}
            <div className="mypay-left-container">
                <MypayProductDetail selectedItemDto={selectedItemDto} />
                <MypayDelivery deliveryDtoList={deliveryDtoList} onSelectItem={setSelectedDeliveryItem}/>
            </div>
            <div className="mypay-right-container">
                <div className="payment-container">
                    <MypayPayment
                        collectPrice={collectPrice}
                        point={point}
                        selectedItemDto={selectedItemDto}
                        onUpdateUsingPoint={onUpdateUsingPoint}
                        selectedDeliveryItem={selectedDeliveryItem}/>
                </div>
            </div>
        </div>
    );
};

export default MypayPane;
