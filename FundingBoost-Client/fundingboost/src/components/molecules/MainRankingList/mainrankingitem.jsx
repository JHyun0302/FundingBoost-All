import React from 'react';
import './mainrankingitem.scss';
import { Link } from 'react-router-dom';
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

const metricLabelMap = {
    funding: '펀딩 반영',
    purchase: '구매 반영',
    wish: '위시 반영',
};

export default function MainRankingItem({ product, metricType }) {
    if (!product) {
        return null;
    }

    const { itemId, itemName, price, itemImageUrl, brandName, score, rank } = product;
    const truncatedTitle = itemName.length > 34 ? `${itemName.slice(0, 34)}...` : itemName;
    const scoreLabel = metricLabelMap[metricType] || '집계 반영';

    return (
        <Link to={`/shopping/detail/${itemId}`} className="main-ranking-card">
            <div className="main-ranking-rank">{rank}</div>
            <div className="main-ranking-image-wrap">
                <img src={toImageProxyUrl(itemImageUrl)} className="main-ranking-image" alt={itemName} />
            </div>
            <div className="main-ranking-meta">
                <div className="main-ranking-brand">{brandName}</div>
                <div className="main-ranking-title">{truncatedTitle}</div>
                <div className="main-ranking-price">{price.toLocaleString()}원</div>
            </div>
            <div className="main-ranking-score">
                <span>{scoreLabel}</span>
                <strong>{score}</strong>
            </div>
        </Link>
    );
}
