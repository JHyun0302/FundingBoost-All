import React, { useEffect, useState } from 'react';
import Button from 'react-bootstrap/Button';
import Modal from 'react-bootstrap/Modal';
import './gifthuboptioncount.scss';

function GifthubOptionCount({ onPersistQuantity, currentQuantity }) {
    const [showModal, setShowModal] = useState(false);
    const [quantity, setQuantity] = useState(String(currentQuantity || 1));

    useEffect(() => {
        setQuantity(String(currentQuantity || 1));
    }, [currentQuantity]);

    const getSanitizedQuantity = () => {
        const parsedQuantity = Number(String(quantity).replace(/\D/g, ''));
        return parsedQuantity > 0 ? parsedQuantity : 1;
    };

    const handleCloseModal = async () => {
        const normalizedQuantity = getSanitizedQuantity();
        if (onPersistQuantity) {
            const isSaved = await onPersistQuantity(normalizedQuantity);
            if (!isSaved) {
                return;
            }
        }

        setShowModal(false);
    };

    const handleShowModal = () => {
        setQuantity(String(currentQuantity || 1));
        setShowModal(true);
    };

    const handleQuantityChange = (e) => {
        setQuantity(e.target.value.replace(/\D/g, ''));
    };

    const adjustQuantity = (delta) => {
        const nextQuantity = Math.max(1, getSanitizedQuantity() + delta);
        setQuantity(String(nextQuantity));
    };

    return (
        <>
            <button className="change-button" onClick={(event) => {
                event.stopPropagation();
                handleShowModal();
            }}>
                변경
            </button>

            <Modal show={showModal} onHide={() => setShowModal(false)} animation={true}>
                <Modal.Header closeButton>
                    <Modal.Title>수량 변경</Modal.Title>
                </Modal.Header>
                <Modal.Body>
                    <div>변경할 수량을 입력하세요.</div>
                    <div className="gifthub-quantity-modal-row">
                        <button
                            type="button"
                            className="gifthub-quantity-adjust-button"
                            onClick={() => adjustQuantity(-1)}
                        >
                            -
                        </button>
                        <input
                            type="text"
                            placeholder="수량 입력"
                            value={quantity}
                            onChange={handleQuantityChange}
                        />
                        <button
                            type="button"
                            className="gifthub-quantity-adjust-button"
                            onClick={() => adjustQuantity(1)}
                        >
                            +
                        </button>
                    </div>
                </Modal.Body>
                <Modal.Footer>
                    <Button variant="secondary" onClick={() => setShowModal(false)}>
                        닫기
                    </Button>
                    <Button variant="primary" onClick={handleCloseModal}>
                        변경 저장
                    </Button>
                </Modal.Footer>
            </Modal>
        </>
    );
}

export default GifthubOptionCount;
