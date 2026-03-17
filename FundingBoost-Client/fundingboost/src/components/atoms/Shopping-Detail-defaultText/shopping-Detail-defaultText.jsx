import React from 'react';
import "./shopping-Detail-defaultText.scss"

const ShoppingDetailDefaultText = () => {
    return (
        <div className="shoppingDetailDefaultText">
            {/*<hr style={{color: 'black', width: '1220px'}}/>*/}
            <div className="shopping-DetailText">
                <hr style={{color: 'black', width: '400px'}}/>
                <div className="shopping-DetailDefaultText">
                    <div className="shopping-Detail-Text-delivery">
                        배송
                    </div>
                    <div className="shopping-Detail-Text-deliveryDetail">
                        <div className="shopping-Detail-delivery-Text1">
                            7만원 이상 구매시 무료배송, 3,000원 우체국택배
                        </div>
                        <div className="shopping-Detail-delivery-Text2">
                            3일이내 도착 예정
                        </div>
                        <div className="shopping-Detail-delivery-Text3">
                            *도착일은 배송지나 배송사 사정 등으로 변경 또는 지연될 수 있습니다.
                        </div>
                    </div>

                </div>
            </div>

        </div>
    );
};

export default ShoppingDetailDefaultText;