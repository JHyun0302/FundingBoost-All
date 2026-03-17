import React, {useCallback, useEffect, useState} from 'react';
import './mypage-delivery-mange.scss';
import MyPageIndex from "../../../molecules/MypageIndex/mypageindex";
import axios from "axios";
import MypageProfile from '../../../molecules/MypageProfile/mypageprofile';
import DeliveryAddressList from "../../../molecules/deliveryList/deliveryList";
import NonMemberModal from "../../../atoms/nonMemberModal/nonMemberModal";

const MypageDeliveryPane = () => {
    const [apiData, setApiData] = useState(null);
    const [modalShowState, setModalShowState] = useState(false);

    const handleButtonClick = (index) => {
        console.log(`Selected index: ${index}`);
    };

    const fetchData = useCallback(async () => {
        try {
            const accessToken = localStorage.getItem('accessToken');
            if (!accessToken) {
                setModalShowState(true);
                return;
            }

            const response = await axios({
                method: 'GET',
                url: `${process.env.REACT_APP_FUNDINGBOOST}/delivery`,
                headers: {
                    "Authorization": `Bearer ${accessToken}`,
                },
                responseType: 'json'
            });
            setApiData(response.data.data);
        } catch (error) {
            console.error("API 호출 중 오류가 발생했습니다.", error);
        }
    }, []);

    useEffect(() => {
        fetchData();
    }, [fetchData]);

    const handleCreateDelivery = async (payload) => {
        const accessToken = localStorage.getItem('accessToken');
        if (!accessToken) {
            setModalShowState(true);
            return { success: false };
        }

        await axios.post(`${process.env.REACT_APP_FUNDINGBOOST}/delivery`, payload, {
            headers: {
                "Content-Type": "application/json",
                "Authorization": `Bearer ${accessToken}`,
            },
        });

        await fetchData();
        return { success: true };
    };

    const handleDeleteDelivery = async (deliveryId) => {
        const accessToken = localStorage.getItem('accessToken');
        if (!accessToken) {
            setModalShowState(true);
            return { success: false };
        }

        await axios.delete(`${process.env.REACT_APP_FUNDINGBOOST}/delivery/${deliveryId}`, {
            headers: {
                "Authorization": `Bearer ${accessToken}`,
            },
        });

        await fetchData();
        return { success: true };
    };

    return (
        <div className="mypage-myhistory-total-container">
            {modalShowState && <NonMemberModal message="로그인 후 펀딩부스트를 시작해보세요." />}
            <div className="mypage-myhistory-left-pane-container">
                <MypageProfile profileInfo={apiData || {}} />
                <MyPageIndex onButtonClick={handleButtonClick} currentPageIndex={4} />
            </div>

            <div className="mypage-myhistory-right-pane-containter">
                <DeliveryAddressList
                    deliveryData={apiData}
                    onCreateDelivery={handleCreateDelivery}
                    onDeleteDelivery={handleDeleteDelivery}
                />
            </div>
        </div>
    );
}

export default MypageDeliveryPane;
