import React, { useEffect, useState } from 'react';
import './mypage-myhistory.scss';
import MypageIndex from '../../../molecules/MypageIndex/mypageindex';
import MypageProfile from '../../../molecules/MypageProfile/mypageprofile';
import MyPageIndex from "../../../molecules/MypageIndex/mypageindex";
import axios from "axios";
import MyPageMyFundingHistoryList from "../../../molecules/MyPageMyFundingHistoryList/mypage-myfunding-history-list";
import NonMemberModal from "../../../atoms/nonMemberModal/nonMemberModal";

const MypageMyHistoryPane = () => {
    const [apiData, setApiData] = useState(null);
    const [modalShowState, setModalShowState] = useState(false);

    const handleButtonClick = (index) => {
        console.log(`Selected index: ${index}`);
    };

    useEffect(() => {
        // API 호출 함수
        const fetchData = async () => {
            try {
                const accessToken = localStorage.getItem('accessToken');
                if (!accessToken) {
                    setModalShowState(true);
                    return;
                }

                const response = await axios({
                    method: 'GET',
                    url: `${process.env.REACT_APP_FUNDINGBOOST}/funding/history`,
                    headers: {
                        "Authorization": `Bearer ${accessToken}`,
                    },
                    responseType: 'json'
                });
                console.log(response.data); // 콘솔에 데이터 출력
                setApiData(response.data.data); // 상태에 데이터 저장

            } catch (error) {
                console.error("API 호출 중 오류가 발생했습니다.", error);
            }
        };

        fetchData();
    }, []);

    return (
        <div className="mypage-myhistory-total-container">
            {modalShowState && <NonMemberModal message="로그인 후 펀딩부스트를 시작해보세요." />}
            <div className="mypage-left-pane-container">
                <MypageProfile profileInfo={apiData || {}} />
                <MyPageIndex onButtonClick={handleButtonClick} currentPageIndex={1} />
            </div>
            <div className="mypage-myhistory-right-pane-containter">
                <div className="mypage-myhistory-container">
                    {apiData && <MyPageMyFundingHistoryList apiData={apiData} />}
                </div>
            </div>
        </div>
    );
}

export default MypageMyHistoryPane;
