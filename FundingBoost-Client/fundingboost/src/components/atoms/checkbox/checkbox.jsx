import React from "react";
import './checkbox.scss';

const Checkbox = ({ isSelected, onCheckboxChange }) => (
    <button
        type="button"
        className={`checkbox-container ${isSelected ? 'is-selected' : ''}`}
        aria-pressed={isSelected}
        aria-label={isSelected ? '선택 해제' : '선택'}
        onClick={(event) => {
            event.stopPropagation();
            onCheckboxChange?.();
        }}
    >
        {isSelected ? '✓' : ''}
    </button>
);

export default Checkbox;
