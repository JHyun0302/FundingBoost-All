import React from 'react';
import "./friendFunding-DropdownBtn.scss";
import DropdownButton from "react-bootstrap/DropdownButton";
import Dropdown from "react-bootstrap/Dropdown";

const SORT_OPTIONS = [
    '최신 등록순',
    '이름순',
    '마감 임박순',
    '달성률 높은순',
    '모금액 높은순'
];

const FriendFundingDropdownBtn = ({ sortOption, onSortChange }) => {
    return (
        <div className="friendFundingDropdownBtn">
            <>
                <DropdownButton
                    id="dropdown-button-dark-example2"
                    variant="secondary"
                    title={sortOption}
                    className="mt-2"
                    data-bs-theme="dark"
                >
                    {SORT_OPTIONS.map((option) => (
                        <Dropdown.Item key={option} onClick={() => onSortChange(option)} active={sortOption === option}>
                            {option}
                        </Dropdown.Item>
                    ))}
                </DropdownButton>
            </>
        </div>
    );
};

export default FriendFundingDropdownBtn;
