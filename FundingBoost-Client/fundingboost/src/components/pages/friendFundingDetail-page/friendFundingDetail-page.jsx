import React, { useEffect, useState } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import axios from "axios";
import Header from "../../organisms/header/header";
import Footer from "../../organisms/footer/footer";
import FriendFundingDetailItem from "../../molecules/FriendFundingDetail/FriendFundingDetail-item/friendFundingDetail-item";
import FriendFundingDetailOptionDetail from "../../molecules/FriendFundingDetail/friendFundingDetail-optionDetail/friendFundingDetail-optionDetail";
import "./friendFundingDetail-page.scss";
import NonMemberModal from "../../atoms/shoppingDetail-nonMemberModal/shoppingDetail-nonMemberModal";

const FriendFundingDetailPage = () => {
    const [friendFundingDetailData, setFriendFundingDetailData] = useState(null);
    const { fundingId } = useParams();
    const navigate = useNavigate();
    const [modalShowState, setModalShowState] = useState(false);
    const [isLoading, setIsLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState("");

    useEffect(() => {
        const fetchData = async () => {
            setIsLoading(true);
            setErrorMessage("");
            setModalShowState(false);

            try {
                const accessToken = localStorage.getItem('accessToken');
                if (!accessToken) {
                    setModalShowState(true);
                    setErrorMessage("로그인 후 친구들의 펀딩을 구경해보세요.");
                    setFriendFundingDetailData(null);
                    return;
                }

                const response = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/funding/friends/${fundingId}`, {
                    responseType: 'json',
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": `Bearer ${accessToken}`
                    },
                });

                if (response?.data?.success === false) {
                    const apiErrorMessage = response?.data?.error?.message || "공유된 펀딩 정보를 불러오지 못했습니다.";
                    if (apiErrorMessage === "잘못된 사용자 접근입니다.") {
                        navigate("/mypage", { replace: true });
                        return;
                    }
                    if (apiErrorMessage === "접근 권한이 없습니다.") {
                        alert("친구 펀딩 목록에서 참여 가능한 펀딩을 확인해주세요.");
                        navigate("/friend-funding", { replace: true });
                        return;
                    }
                    setErrorMessage(apiErrorMessage);
                    setFriendFundingDetailData(null);
                    return;
                }

                setFriendFundingDetailData(response.data);
            } catch (error) {
                const apiErrorMessage = error?.response?.data?.error?.message;
                if (error?.response?.status === 401) {
                    setModalShowState(true);
                    setErrorMessage("로그인이 만료되었습니다. 다시 로그인해주세요.");
                    setFriendFundingDetailData(null);
                    return;
                }
                if (error?.response?.status === 403) {
                    alert("친구 펀딩 목록에서 참여 가능한 펀딩을 확인해주세요.");
                    navigate("/friend-funding", { replace: true });
                    return;
                }

                setErrorMessage(apiErrorMessage || "공유된 펀딩 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
                setFriendFundingDetailData(null);
            } finally {
                setIsLoading(false);
            }
        };

        fetchData();
    }, [fundingId, navigate]);

    const closeNonMemberModal = () => {
        setModalShowState(false);
        navigate(-1);
    };

    const hasFundingData = Boolean(friendFundingDetailData?.data);

    return (
        <div className="friendFundingDetail-Page">
            <Header />
            <NonMemberModal
                message="로그인이 필요한 서비스입니다."
                show={modalShowState}
                onClose={closeNonMemberModal}
            />
            {isLoading && (
                <div className="friendFundingDetail-status">펀딩 정보를 불러오는 중입니다.</div>
            )}
            {!isLoading && errorMessage && (
                <div className="friendFundingDetail-status is-error">{errorMessage}</div>
            )}
            {!isLoading && !errorMessage && !hasFundingData && (
                <div className="friendFundingDetail-status">공유된 펀딩 정보를 찾을 수 없습니다.</div>
            )}
            {!isLoading && !errorMessage && hasFundingData && (
                <div className="friendFundingDetail">
                    <FriendFundingDetailItem friendFundingDetailData={friendFundingDetailData} />
                    <FriendFundingDetailOptionDetail friendFundingDetailData={friendFundingDetailData} />
                </div>
            )}
            <Footer />
        </div>
    );
};

export default FriendFundingDetailPage;
