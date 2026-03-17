import React, { useState } from 'react';
import './mypage-myfunding-dofunding.scss';
import MyfundingAdditionalPane from "../../atoms/mypage-additional-info/mypage-additional-info";
import MyfundingItemList from "../mypage-myfunding-itemlist/mypage-myfunding-itemlist";
import messagebox from "../../../assets/messagebox.svg";
import MyPageMyFundingMessage from "../Modal/MypageMyfundingMessage/mypagemyfundingmessage"
import messageboxopen from "../../../assets/messagebox-open.svg"

const MyfundingFinFundingPane = ({ apiData, deadline, deadlineDate, totalPercent, message, tag, participateFriendDtoList, myPageFundingItemDtoList}) => {
    const [showModal, setShowModal] = useState(false); // 모달 열림 여부 상태 관리
    const [isHovered, setIsHovered] = useState(false);

    const handleHover = () => {
        setIsHovered(!isHovered);
    };

    const toggleModal = () => {
        setShowModal(!showModal);
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
                    <div className="mypage-fin-button-noti-wrpper">
                        ＊ 펀딩이 종료되었습니다.
                    </div>
                </div>
                <div className="horizontalLine"></div>
                <MyfundingItemList myPageFundingItemDtoList={myPageFundingItemDtoList} apiData={apiData}/>
                <div className="horizontalLine"></div>
                <MyfundingAdditionalPane participateFriendDtoList={participateFriendDtoList} totalPercent={totalPercent}/>
            </div>
            <MyPageMyFundingMessage show={showModal} message={message} handleClose={toggleModal} />
        </div>
    );
}

export default MyfundingFinFundingPane;
