import React from 'react';
import ShoppingNowBtn from '../../../atoms/button/ShoppingNowBtn/shoppingNowBtn';
import './gifthubNonItem.scss';
import NonItemImg from '../../../../assets/nonItemImg.svg';

const GifthubNonItem = () => {
    return (
        <div className="GifthubNonItem">
            <hr style={{border: '1px solid', color: 'black', width: '100%'}}/>
            <div className="gifthubNonItemText">
                <div className="GifthubNonItem-Title"> GIFTHUB에 담긴 상품이 없어요</div>
                <img src={NonItemImg} style={{width:'150px'}}/>
                <div className="gifthubNonItem-text">상품을 담아 펀딩을 참여해보세요!</div>
                <ShoppingNowBtn/>
            </div>

            <hr style={{border: '1px solid', color: 'black', width: '100%'}}/>

        </div>
    );
};

export default GifthubNonItem;