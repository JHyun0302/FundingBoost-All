import React, { useState } from 'react';
import axios from 'axios';
import './mypage-myfunding-dofunding.scss';
import MyfundingAdditionalPane from "../../atoms/mypage-additional-info/mypage-additional-info";
import MyfundingItemList from "../mypage-myfunding-itemlist/mypage-myfunding-itemlist";
import messagebox from "../../../assets/messagebox.svg";
import MyPageMyFundingMessage from "../Modal/MypageMyfundingMessage/mypagemyfundingmessage";
import messageboxopen from "../../../assets/messagebox-open.svg";
import ExtensionButton from "../../atoms/buttons/Mypage-Myfunding-Button/mypage-myfunding-extension-button/mypage-myfunding-extension-button";

const MyfundingDoFundingPane = ({ apiData, deadline, deadlineDate, totalPercent, message, tag, participateFriendDtoList, myPageFundingItemDtoList, isFundingClosed, setIsFundingClosed }) => {
    const [showModal, setShowModal] = useState(false); // 모달 열림 여부 상태 관리
    const [isHovered, setIsHovered] = useState(false);

    const toggleModal = () => {
        setShowModal(!showModal);
    };

    const handleHover = () => {
        setIsHovered(!isHovered);
    };

    const handleCloseFunding = async () => {
        const fundingId = myPageFundingItemDtoList[0]?.fundingId;
        try {
            const accessToken = localStorage.getItem('accessToken');

            const response = await axios.post(`${process.env.REACT_APP_FUNDINGBOOST}/funding/close/${fundingId}`, null, {
                responseType: 'json',
                headers: {
                    'Content-Type': 'application/json',
                    "Authorization": `Bearer ${accessToken}`,
                }
            });
            console.log('POST 결과:', response.data);
            setIsFundingClosed(true); // 펀딩 종료 상태 변경
            console.log("isFundingClosed 업데이트 전:", isFundingClosed);

        } catch (error) {
            console.error('POST 에러:', error);
        }
    };

    return (
        <div className="mypage-right-pane-containter">
            <div className="mypage-myfunding-content-wrapper">
                <div className="mypage-myfunding-title-wrapper">
                    <div className="mypage-day-wrpper">
                        <div className="mypage-myfunding-title">MY 펀딩</div>
                        <div className="memberFundingDday">{deadlineDate}</div>
                        <div className="mypage-enddate-pane">~ {deadline}</div>
                        <div className="mypage-tag-pane">{tag}</div>
                        <button
                            className="mypage-message-btn"
                            onClick={toggleModal}
                            onMouseEnter={handleHover}
                            onMouseLeave={handleHover}
                        >
                            <img
                                src={isHovered ? messageboxopen : messagebox}
                                alt="messagebox"
                                className="mypage-message-icon"
                            />
                        </button>
                    </div>
                    <div className="mypage-button-noti-wrpper">
                        <div className="mypage-action-button-row">
                            <ExtensionButton myPageFundingItemDtoList={myPageFundingItemDtoList} />
                        </div>
                        <div className="mypage-remain-date-noti">
                            <div>＊ 기간은 자동 2주 연장됩니다.</div>
                        </div>
                    </div>
                </div>
                <div className="horizontalLine"></div>
                <MyfundingItemList myPageFundingItemDtoList={myPageFundingItemDtoList} isFundingClosed={isFundingClosed} setIsFundingClosed={setIsFundingClosed} />
                <div className="mypage-end-button-container">
                    <button className="mypage-button-style-03" onClick={handleCloseFunding}>종료하기</button>
                </div>
                <div className="horizontalLine"></div>
                <MyfundingAdditionalPane participateFriendDtoList={participateFriendDtoList} totalPercent={totalPercent} />
            </div>
            <MyPageMyFundingMessage show={showModal} message={message} handleClose={toggleModal} />
        </div>
    );
}

export default MyfundingDoFundingPane;
