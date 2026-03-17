import React from 'react';
import './mypagemyfundingmessage.scss'
import Modal from 'react-bootstrap/Modal';
import Button from 'react-bootstrap/Button';

function MyPageMyFundingMessage({ show, message, handleClose }) {
    return (
        <Modal show={show} onHide={handleClose} animation={true}>
            <Modal.Header closeButton>
                <Modal.Title>펀딩 메세지 ✉️</Modal.Title>
            </Modal.Header>
            <Modal.Body>
                <p>{message}</p>
            </Modal.Body>
            <Modal.Footer>
                <Button variant="secondary" onClick={handleClose}>
                    닫기
                </Button>
            </Modal.Footer>
        </Modal>
    );
}

export default MyPageMyFundingMessage;
