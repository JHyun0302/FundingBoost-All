import React from 'react';
import './shopping-item-category.scss';

export default function ShoppingCategory({ categories, selectedCategoryName, onCategorySelect }) {
    const handleCategorySelect = (category) => {
        onCategorySelect(category); // 카테고리 객체를 부모 컴포넌트로 전달
    };

    return (
        <div className="item-category-button-wrapper">
            {categories.map((category) => (
                <button
                    key={category.param}
                    className={`item-category-button-style ${selectedCategoryName === category.name ? 'selected' : ''}`}
                    onClick={() => handleCategorySelect(category)}
                >
                    {category.name}
                </button>
            ))}
        </div>
    );
}
