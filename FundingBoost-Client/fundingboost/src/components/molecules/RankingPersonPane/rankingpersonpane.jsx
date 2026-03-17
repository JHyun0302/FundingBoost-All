import React, { useState } from 'react';
import './rankingpersonpane.scss';
import AllButton from "../../atoms/buttons/MainRankingButton/AllButton/allbutton";
import ManButton from "../../atoms/buttons/MainRankingButton/ManButton/manbutton";
import WomanButton from "../../atoms/buttons/MainRankingButton/WomanButton/womanbutton";

const RankingPersonPane = () => {
    const [selectedButton, setSelectedButton] = useState('all'); // 선택된 버튼의 타입을 저장하는 상태

    const handleButtonClick = (buttonType) => {
        setSelectedButton(buttonType); // 클릭된 버튼의 타입으로 선택 상태 변경
    };

    return (
        <div className={`ranking-pane-container ${selectedButton}`}>
            <AllButton
                isSelected={selectedButton === 'all'} // 현재 선택된 버튼인지 여부를 isSelected prop으로 전달
                onClick={() => handleButtonClick('all')} // 클릭 이벤트 핸들러를 전달
            />
            <ManButton
                isSelected={selectedButton === 'man'}
                onClick={() => handleButtonClick('man')}
            />
            <WomanButton
                isSelected={selectedButton === 'woman'}
                onClick={() => handleButtonClick('woman')}
            />
        </div>
    );
}

export default RankingPersonPane;
