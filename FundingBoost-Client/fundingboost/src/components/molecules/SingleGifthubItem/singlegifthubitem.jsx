import React from 'react';
import './singlegifthubitem.scss';
import './../../organisms/contents/gifthub/gifthub';
import Checkbox from '../../atoms/checkbox/checkbox';
import axios from 'axios';
import GifthubOptionCount from '../Modal/GifthubOptionCount/gifthuboptioncount';
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

export default function SingleGiftHubItem({ item, isSelected, onCheckboxChange, onDelete, onQuantityChange }) {
    const gifthubItemId = item.giftHubItemId;

    const handleCheckboxChange = () => {
        if (onCheckboxChange) {
            onCheckboxChange(gifthubItemId, !isSelected);
        }
    };

    const handleDeleteItem = async () => {
        try {
            const accessToken = localStorage.getItem('accessToken');
            await axios.delete(`${process.env.REACT_APP_FUNDINGBOOST}/gifthub/${gifthubItemId}`, {
                headers: {
                    'Content-Type': 'application/json',
                    'Authorization': `Bearer ${accessToken}`,
                },
            });
        } catch (error) {
            console.error('아이템 삭제 에러:', error);
            return;
        }

        if (onDelete) {
            onDelete(gifthubItemId);
        }
    };

    const handleQuantityChange = (newQuantity) => {
        if (onQuantityChange) {
            onQuantityChange(gifthubItemId, newQuantity);
        }
    };

    const persistQuantity = async (newQuantity) => {
        const normalizedQuantity = Math.max(1, Number(newQuantity) || 1);

        try {
            const accessToken = localStorage.getItem('accessToken');
            await axios.patch(
                `${process.env.REACT_APP_FUNDINGBOOST}/gifthub/quantity/${gifthubItemId}`,
                { quantity: normalizedQuantity },
                {
                    responseType: 'json',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${accessToken}`,
                    },
                },
            );
            handleQuantityChange(normalizedQuantity);
            return true;
        } catch (error) {
            console.error('PATCH 에러:', error);
            return false;
        }
    };

    const adjustQuantity = async (delta, event) => {
        event.stopPropagation();
        await persistQuantity(item.quantity + delta);
    };

    return (
        <div className="giftbox-total-container" onClick={handleCheckboxChange} role="button" tabIndex={0} onKeyDown={(event) => {
            if (event.key === "Enter" || event.key === " ") {
                event.preventDefault();
                handleCheckboxChange();
            }
        }}>
            <div className="checkbox-item-container">
                <Checkbox
                    isSelected={isSelected}
                    onCheckboxChange={handleCheckboxChange}
                />
                <div className="gifthub-fundingRegistItem">
                    <img src={toImageProxyUrl(item.itemImageUrl)} alt={item.itemName} className="sequenceGroup" />
                    <div className="gifthub-itemDetail">
                        <div className="gifthub-title">{item.itemName}</div>
                        <button className="delete-button" onClick={(event) => {
                            event.stopPropagation();
                            handleDeleteItem();
                        }}>삭제</button>
                        <div className="gifthub-optionDetail">
                            <div className="gifthub-optionGroup">
                                <div className="gifthub-option">옵션</div>
                            </div>
                            <div className="gifthub-optionName">{item.optionName}</div>
                        </div>
                        <div className="quantity-container">
                            <div className="quantity-text">수량</div>
                            <div className="quantity-number">{item.quantity}</div>
                            <div className="quantity-stepper" onClick={(event) => event.stopPropagation()}>
                                <button
                                    type="button"
                                    className="quantity-step-button"
                                    aria-label="수량 증가"
                                    onClick={(event) => adjustQuantity(1, event)}
                                >
                                    ▲
                                </button>
                                <button
                                    type="button"
                                    className="quantity-step-button"
                                    aria-label="수량 감소"
                                    onClick={(event) => adjustQuantity(-1, event)}
                                >
                                    ▼
                                </button>
                            </div>
                            <GifthubOptionCount
                                onQuantityChange={handleQuantityChange}
                                currentQuantity={item.quantity}
                                onPersistQuantity={persistQuantity}
                            />
                        </div>
                        <div className="giftbox-item-price">{(item.itemPrice * item.quantity).toLocaleString()} 원</div>
                    </div>
                </div>
            </div>
        </div>
    );
}
