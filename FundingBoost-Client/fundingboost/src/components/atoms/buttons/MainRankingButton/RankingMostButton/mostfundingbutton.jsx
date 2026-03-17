// RankingMostButton.jsx
import React from 'react';
import './mostfundingbutton.scss';

const RankingMostButton = ({ text, isSelected, onClick }) => {
    return (
        <button
            className={`most-funding-button ${isSelected ? 'selected' : ''}`} // isSelected prop을 사용하여 선택 여부를 확인하고 클래스를 추가
            onClick={onClick} // 클릭 이벤트 핸들러를 전달
        >
            {text}
        </button>
    );
}

export default RankingMostButton;
