import React, { useState } from 'react';
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import axios from 'axios';
import './checkexchangepoint.scss';

function ExchangePoint({ item, memberId }) {
    const [showModal, setShowModal] = useState(false);

    const handleExchange = async () => {
        const requestData = {
            fundingId: item.fundingId
        };

        try {
            const accessToken = localStorage.getItem('accessToken');

            await axios.patch(`${process.env.REACT_APP_FUNDINGBOOST}/member/point`, requestData, {
                responseType: 'json',
                headers: {
                    'Content-Type': 'application/json',
                    "Authorization": `Bearer ${accessToken}`,
                },
            });
            setShowModal(false); // 요청 성공 후 모달 닫기
            window.location.reload(); // 페이지 새로 고침
        } catch (error) {
            console.error('PATCH 에러:', error);
        }
    };

    return (
        <>
            <button className="exchange-button" onClick={() => setShowModal(true)}>
                포인트 전환하기
            </button>

            <Modal show={showModal} onHide={() => setShowModal(false)} animation={true}>
                <Modal.Header closeButton>
                    <Modal.Title>포인트 전환</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div>포인트를 전환하시겠습니까?</div>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowModal(false)}>
                        닫기
                    </Button>
                    <Button variant="primary" onClick={handleExchange}>
                        전환하기
                    </Button>
                </Modal.Footer>
            </Modal>
        </>
    );
}

export default ExchangePoint;
