import React, { useEffect, useState } from 'react';
import { useLocation, useParams } from 'react-router-dom';
import PointUse from '../../atoms/point/pointUse';
import './friendFundingPay-page.scss';
import axios from "axios";
import HeaderBar from "../../organisms/header/header";
import Footer from "../../organisms/footer/footer";
import FriendFundingPayPrice from "../../molecules/FriendFundingPay/FriendFundingPay-Price/friendFundingPay-Price";
import FriendFundingPayBarcode from "../../molecules/FriendFundingPay/FriendFundingPay-barcode/friendFundingPay-barcode";
import FriendFundingPayProfile from "../../molecules/FriendFundingPay/FriendFundingPay-profile/friendFundingPay-profile";
import FriendFundingPayCurrentPay from "../../molecules/FriendFundingPay/FriendFundingPay-CurrentPay/friendFundingPay-CurrentPay";
import NonMemberModal from "../../atoms/nonMemberModal/nonMemberModal";

const FriendFundingPayPage = () => {
    const [modalShowState, setModalShowState] = useState(false);
    const location = useLocation();
    const fundingAmount = Number(location.state || 0);
    const [usePoints, setUsePoints] = useState("");
    const [finalPrice, setFinalPrice] = useState("");
    const { fundingId } = useParams();
    const [friendFundingPayData, setFriendFundingPayData] = useState(null);

    const updateusePoints = (points) => {
        setUsePoints(points);
    };

    useEffect(() => {
        const fetchData = async () => {
            try {
                const accessToken = localStorage.getItem('accessToken');
                if (!accessToken) {
                    setModalShowState(true);
                    return;
                }

                const response = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/pay/friends/${fundingId}`, {
                    responseType: 'json',
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": `Bearer ${accessToken}`
                    },
                });

                setFriendFundingPayData(response.data.data);
            } catch (error) {
                console.error("Error data:", error);
            }
        };

        fetchData();
    }, [fundingId]);

    return (
        <div className="friendFundingPayPage">
            <HeaderBar />
            {modalShowState && <NonMemberModal message="로그인 후 친구들의 펀딩을 구경해보세요." />}
            {friendFundingPayData && (
                <div className="friendFundingPayPageContent">
                    <div className="friendFundingPayPageCard">
                        <div className="friendFundingProfile">
                            <FriendFundingPayProfile friendFundingPayData={friendFundingPayData} />
                        </div>
                        <div className="friendFundingPayDivider" />
                        <FriendFundingPayPrice friendFundingPayData={friendFundingPayData} />
                        <div className="friendFundingPayDivider dashed" />
                        <PointUse
                            friendFundingPayData={friendFundingPayData}
                            fundingAmount={fundingAmount}
                            onUpdatePoints={updateusePoints}
                        />
                        <div className="friendFundingPayDivider dashed" />
                        <FriendFundingPayCurrentPay
                            friendFundingPayData={friendFundingPayData}
                            fundingAmount={fundingAmount}
                            usePoints={usePoints}
                            onUpdateFinalPrice={setFinalPrice}
                        />
                        <div className="friendFundingPayDivider dashed" />
                        <FriendFundingPayBarcode
                            friendFundingPayData={friendFundingPayData}
                            finalPrice={finalPrice}
                            fundingAmount={fundingAmount}
                            usePoints={usePoints}
                            fundingId={fundingId}
                        />
                        <div className="friendFundingPayPageTear" />
                    </div>
                </div>
            )}
            <Footer />
        </div>
    );
};

export default FriendFundingPayPage;
