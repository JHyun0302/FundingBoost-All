import React from "react";
import { Link } from "react-router-dom";
import './paysuccessbutton.scss';

function PaySuccessBtn() {
    return (
        <Link to="/mypage" className="pay-success-button">마이페이지로 이동</Link>
    );
}

export default PaySuccessBtn;
