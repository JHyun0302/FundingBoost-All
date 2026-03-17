import React from "react";
import { Link } from "react-router-dom";
import './error-btn.scss';

function ErrorBtn() {
    return (
        <Link to="/home" className="pay-success-button">홈으로 이동</Link>
    );
}

export default ErrorBtn;
