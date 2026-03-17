import React from 'react';
import './gifthubModal.scss';

const FundingRegistPageModal = ({ show, onClose, message, onGiftHub, itemName }) => {
    if (!show) {
        return null;
    }
    return (
        <div className="modal-boby">
            <div className="modal">
                <div className="modal-content">
                    <div className="modal-header">
                        <h3>{message}</h3>
                    </div>

                    <div className="modal-message1">&lt; {itemName} &gt;가 <br/>장바구니에 담겼습니다.</div>
                    <div className="modal-message2">쇼핑을 계속 하시겠습니까?</div>
                    <div className="modal-button">
                        <button className="modal-myPageBtn" onClick={onClose}>계속쇼핑하기</button>
                        <button className="modal-closeBtn" onClick={onGiftHub}>GiftHub로이동</button>
                    </div>


                </div>
            </div>
        </div>
    );
};

export default FundingRegistPageModal;