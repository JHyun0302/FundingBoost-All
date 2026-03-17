import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './nonMemberModal.scss';

const NonMemberModal = ({message}) => {
    const navigate = useNavigate();
    const [showModal, setShowModal] = useState(false);
    const isAuthenticated = localStorage.getItem('isAuthenticated') === 'true';
    useEffect(() => {
            setShowModal(!isAuthenticated);
    }, [isAuthenticated]);

    const closeModal = () => {
        setShowModal(false);
        navigate(-1); //이전 페이지로 이동
    };

    const loginBtnModal = () => {
        setShowModal(false);
        navigate('/login');
    };

    return (
        <div className={`modal-boby ${showModal ? 'showModal' : ''}`}>
            <div className="modal">
                <div className="modal-content">
                    <div className="modal-header">
                        <h3>로그인이 필요한 서비스입니다.</h3>
                    </div>
                    <div className="modal-message1">{message}</div>
                    <div className="modal-message2">로그인하시겠습니까?</div>
                    <div className="modal-button">
                        <button className="modal-myPageBtn" onClick={loginBtnModal}>로그인</button>
                        <button className="modal-closeBtn" onClick={closeModal}>닫기</button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default NonMemberModal;
