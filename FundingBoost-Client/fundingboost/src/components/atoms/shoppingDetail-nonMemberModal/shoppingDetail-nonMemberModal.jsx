import React, { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import './shoppingDetail-nonMemberModal.scss';

const NonMemberModal = ({message, onClose, show }) => {
    const navigate = useNavigate();

    if (!show) {
        return null;
    }
    const loginClick =()=>{
        navigate('/login');
    }

    return (
        // <div className={`shoppingNonMember-modal-boby ${showModal ? 'showModal' : ''}`}>
        <div className={"shoppingNonMember-modal-body"}>
            <div className="shoppingNonMember-modal">
                <div className="shoppingNonMember-modal-content">
                    <div className="shoppingNonMember-">
                        <h3>{message}</h3>
                        <hr style={{color: 'black', width: '100%'}}/>
                    </div>
                    <div className="shoppingNonMember-modal-message2">로그인하시겠습니까?</div>
                    <div className="shoppingNonMember-modal-button">
                        <button className="shoppingNonMember-modal-myPageBtn" onClick={loginClick}>로그인</button>
                        <button className="shoppingNonMember-modal-closeBtn" onClick={onClose}>닫기</button>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default NonMemberModal;
