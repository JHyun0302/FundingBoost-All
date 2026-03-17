import React, { useEffect, useState } from 'react';
import HeaderBar from "../../organisms/header/header";
import '@fortawesome/fontawesome-free/css/all.min.css';
import 'mdb-react-ui-kit/dist/css/mdb.min.css';
import Footer from '../../organisms/footer/footer';
import MainPane from '../../organisms/contents/main/main';
import MainFunding from "../../molecules/mainFunding/mainFunding";
import GenderSetupModal from "../../atoms/genderSetupModal/genderSetupModal";
import axios from "axios";

const getStoredAccessToken = () => {
    const token = localStorage.getItem('accessToken');
    if (!token) {
        return '';
    }

    const trimmedToken = token.trim();
    if (!trimmedToken || trimmedToken === 'null' || trimmedToken === 'undefined') {
        return '';
    }

    try {
        const [, payload] = trimmedToken.split('.');
        if (!payload) {
            clearStoredAuth();
            return '';
        }

        const normalizedPayload = payload.replace(/-/g, '+').replace(/_/g, '/');
        const decodedPayload = JSON.parse(atob(normalizedPayload));
        if (decodedPayload?.exp && Date.now() >= decodedPayload.exp * 1000) {
            clearStoredAuth();
            return '';
        }
    } catch (error) {
        clearStoredAuth();
        return '';
    }

    return trimmedToken;
};

const clearStoredAuth = () => {
    localStorage.removeItem('accessToken');
    localStorage.removeItem('refreshToken');
};

function MainPage() {
    const [mainData, setMainData] = useState({});
    const [isLoading, setIsLoading] = useState(false);
    const [needsGenderSetup, setNeedsGenderSetup] = useState(false);
    const [selectedGender, setSelectedGender] = useState('');
    const [genderSaveLoading, setGenderSaveLoading] = useState(false);
    const [genderError, setGenderError] = useState('');

    const fetchData = async () => {
        if (isLoading) return;
        setIsLoading(true);

        try {
            const url = `${process.env.REACT_APP_FUNDINGBOOST}/home`;
            const accessToken = getStoredAccessToken();
            let response;

            try {
                response = await axios.get(url, {
                    responseType: 'json',
                    headers: accessToken ? {
                        "Content-Type": "application/json",
                        "Authorization": `Bearer ${accessToken}`,
                    } : {
                        "Content-Type": "application/json",
                    },
                });
            } catch (error) {
                if (error?.response?.status !== 401 || !accessToken) {
                    throw error;
                }

                clearStoredAuth();
                setNeedsGenderSetup(false);

                response = await axios.get(url, {
                    responseType: 'json',
                    headers: {
                        "Content-Type": "application/json",
                    },
                });
            }

            const data = response.data;
            setMainData(data);
            console.log(url)

            const homeMemberInfo = data.data?.homeMemberInfoDto;
            if (homeMemberInfo && homeMemberInfo.nickName) {
                console.log("nickName: " + homeMemberInfo.nickName);
                localStorage.setItem('nickName', homeMemberInfo.nickName);
            } else {
                console.warn("Warning: homeMemberInfoDto or nickName is null or undefined.");
            }
        } catch (error) {
            console.error("Error fetching data:", error);
        } finally {
            setIsLoading(false);
        }
    };

    const fetchMemberGenderStatus = async () => {
        const accessToken = getStoredAccessToken();

        if (!accessToken) {
            setNeedsGenderSetup(false);
            return;
        }

        try {
            const response = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/member/gender`, {
                responseType: 'json',
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${accessToken}`,
                },
            });

            const status = response.data?.data;
            const needsSetup = Boolean(status?.needsSetup);

            setNeedsGenderSetup(needsSetup);
            setSelectedGender(needsSetup ? '' : (status?.gender || ''));
            setGenderError('');
        } catch (error) {
            if (error?.response?.status === 401) {
                clearStoredAuth();
            }
            console.warn('성별 설정 상태를 확인하지 못했습니다.', error);
            setNeedsGenderSetup(false);
        }
    };

    const handleSaveGender = async () => {
        const accessToken = getStoredAccessToken();

        if (!selectedGender) {
            setGenderError('성별을 선택한 뒤 저장해주세요.');
            return;
        }

        if (!accessToken) {
            setGenderError('로그인 정보를 다시 확인해주세요.');
            return;
        }

        setGenderSaveLoading(true);
        setGenderError('');

        try {
            await axios.patch(`${process.env.REACT_APP_FUNDINGBOOST}/member/gender`, {
                gender: selectedGender,
            }, {
                responseType: 'json',
                headers: {
                    "Content-Type": "application/json",
                    "Authorization": `Bearer ${accessToken}`,
                },
            });

            setNeedsGenderSetup(false);
        } catch (error) {
            if (error?.response?.status === 401) {
                clearStoredAuth();
                setNeedsGenderSetup(false);
                setGenderError('로그인이 만료되었습니다. 다시 로그인해주세요.');
                return;
            }
            console.error('성별 저장에 실패했습니다.', error);
            setGenderError('저장에 실패했습니다. 잠시 후 다시 시도해주세요.');
        } finally {
            setGenderSaveLoading(false);
        }
    };

    useEffect(() => {
        fetchData();
        fetchMemberGenderStatus();
    }, []);

    return (
        <div className="Main-pages">
            <HeaderBar />
            <MainFunding mainData={mainData} />
            <MainPane />
            <GenderSetupModal
                show={needsGenderSetup}
                nickName={mainData?.data?.homeMemberInfoDto?.nickName || localStorage.getItem('nickName')}
                selectedGender={selectedGender}
                onSelect={setSelectedGender}
                onSubmit={handleSaveGender}
                isSaving={genderSaveLoading}
                errorMessage={genderError}
            />
            <Footer />
        </div>
    );
}

export default MainPage;
