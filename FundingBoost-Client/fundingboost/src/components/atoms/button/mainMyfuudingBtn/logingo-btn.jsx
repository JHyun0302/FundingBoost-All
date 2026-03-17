//메인페이지 '로그인하러가기' 버튼
import React from 'react';
import Button from "react-bootstrap/Button";
import  '../yellowBtn.scss';

const LoginButton = ({ onClick }) => {
    return (
        <>
            <div className="logingo-btn">
                <Button href="/login" className="yellowBtn">로그인 바로가기</Button>{' '}
            </div>
        </>
    );
}

export default LoginButton;