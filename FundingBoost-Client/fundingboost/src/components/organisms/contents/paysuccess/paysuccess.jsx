import React from 'react';
import '../paysuccess/paysuccess.scss';
import Paysuccess_icon from '../../../../assets/pay-success.svg';
import PaySuccessBtn from "../../../atoms/buttons/PaysuccessButton/paysuccessubutton";

const PaySuccessPane = () => {
    return (
        <div className="success-pane-container">
            <div className="success-horizontalLine-up"></div>
            <img src={Paysuccess_icon} alt="Pay Success" className="success-icon" />
            <div className="pay-success-text">
                💰 결제가 완료되었습니다 💰
            </div>
            <PaySuccessBtn/>
            <div className="success-horizontalLine-down"></div>
        </div>
    );
}

export default PaySuccessPane;
