import React from 'react';
import './fundingsuccess.scss';
import Fundingsuccess_icon from '../../../../assets/fundingsuccessicon.svg';
import PaySuccessBtn from "../../../atoms/buttons/PaysuccessButton/paysuccessubutton";

const PaySuccessPane = () => {
    return (
        <div className="success-pane-container">
            <div className="success-horizontalLine-up"></div>
            <img src={Fundingsuccess_icon} alt="Funding Success" className="success-icon" />
            <div className="pay-success-text">
                 📌 펀딩 등록이 완료되었습니다 📌
            </div>
            <PaySuccessBtn/>
            <div className="success-horizontalLine-down"></div>
        </div>
    );
}

export default PaySuccessPane;
