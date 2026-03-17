// RankingItemPane.jsx
import React, { useState } from 'react';
import './rankingitempane.scss';
import MostFundingButton from "../../atoms/buttons/MainRankingButton/RankingMostButton/mostfundingbutton";

const RankingItemPane = () => {
    const [selectedButton, setSelectedButton] = useState('funding'); // 선택된 버튼의 타입을 저장하는 상태

    const handleButtonClick = (buttonType) => {
        setSelectedButton(buttonType); // 클릭된 버튼의 타입으로 선택 상태 변경
    };

    return (
        <div className="ranking-item-pane-container">
            <div className="balloon">
                <div className="most-item-category-container">
                    <MostFundingButton
                        text="많이 펀딩한"
                        isSelected={selectedButton === 'funding'} // 현재 선택된 버튼인지 여부를 isSelected prop으로 전달
                        onClick={() => handleButtonClick('funding')} // 클릭 이벤트 핸들러를 전달
                    />
                    <MostFundingButton
                        text="많이 구매한"
                        isSelected={selectedButton === 'purchase'}
                        onClick={() => handleButtonClick('purchase')}
                    />
                    <MostFundingButton
                        text="많이 위시에 담은"
                        isSelected={selectedButton === 'wish'}
                        onClick={() => handleButtonClick('wish')}
                    />
                </div>
            </div>
        </div>
    );
}

export default RankingItemPane;
