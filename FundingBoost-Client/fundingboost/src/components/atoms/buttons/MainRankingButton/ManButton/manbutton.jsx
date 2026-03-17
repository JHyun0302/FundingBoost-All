// ManButton.jsx
import React, { useState } from 'react';
import '../AllButton/allbutton.scss';
import manRankingIcon from '../../../../../assets/ranking/manranking.png';

const ManButton = ({ isSelected, onClick }) => {
    return (
        <div className="button-container">
            <button
                className={`circle-button ${isSelected ? 'selected' : ''}`} // isSelected prop을 사용하여 선택 여부를 확인하고 클래스를 추가
                onClick={onClick} // 클릭 이벤트 핸들러를 전달
            >
                <img src={manRankingIcon} alt="Man Ranking" className="button-icon" />
                <p className="button-label">남자가</p>
            </button>
        </div>
    );
}

export default ManButton;
