import React from 'react';
import './error-handle.scss';
import error_icon from '../../../../assets/error.svg';
import ErrorBtn from "../../../atoms/buttons/ErrorButton/error-btn";

const PaySuccessPane = () => {
    return (
        <div className="success-pane-container">
            <div className="error-horizontalLine-up"></div>
            <img src={error_icon} alt="error_icon" className="error_icon" />
            <div className="pay-success-text">
                ⚠️ 네트워크 에러가 발생했습니다. 잠시후 다시 시도해주세요.⚠️
            </div>
            <ErrorBtn/>
            <div className="error-horizontalLine-down"></div>
        </div>
    );
}

export default PaySuccessPane;
