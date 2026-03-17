import React, { useEffect, useState } from 'react';
import './main.scss';
import MainRankingItem from "../../../molecules/MainRankingList/mainrankingitem";
import axios from "axios";

const RANKING_TYPES = [
    { label: '많이 펀딩한', value: 'funding' },
    { label: '많이 구매한', value: 'purchase' },
    { label: '많이 위시에 담은', value: 'wish' },
];

const AUDIENCE_TYPES = [
    { label: '모두', value: 'all', badge: 'ALL', iconClass: '' },
    { label: '여성', value: 'woman', badge: '', iconClass: 'fa-solid fa-venus' },
    { label: '남성', value: 'man', badge: '', iconClass: 'fa-solid fa-mars' },
];

const PRICE_RANGES = [
    { label: '전체 가격', value: 'all' },
    { label: '1만원 이하', value: 'under10k' },
    { label: '1-3만원', value: '10kto30k' },
    { label: '3-5만원', value: '30kto50k' },
    { label: '5만원 이상', value: 'over50k' },
];

const MainPane = () => {
    const [selectedRankingType, setSelectedRankingType] = useState('funding');
    const [selectedAudience, setSelectedAudience] = useState('all');
    const [selectedPriceRange, setSelectedPriceRange] = useState('all');
    const [rankingItems, setRankingItems] = useState([]);
    const [isLoading, setIsLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState('');
    const [showRankingGuide, setShowRankingGuide] = useState(false);

    useEffect(() => {
        const fetchRankings = async () => {
            setIsLoading(true);
            setErrorMessage('');

            try {
                const apiV3Base = process.env.REACT_APP_FUNDINGBOOST_V3 || "/api/v3";
                const params = new URLSearchParams({
                    rankingType: selectedRankingType,
                    audience: selectedAudience,
                    priceRange: selectedPriceRange,
                    size: '12',
                });

                const response = await axios.get(`${apiV3Base}/home/rankings?${params.toString()}`);
                const fallbackApplied = response?.headers?.['x-fundingboost-fallback-applied'] === 'true';
                const isLocalhost = window.location.hostname === 'localhost' || window.location.hostname === '127.0.0.1';

                if (isLocalhost && fallbackApplied) {
                    console.log('fallback 동작중!');
                }

                const items = Array.isArray(response?.data?.data) ? response.data.data : [];
                setRankingItems(items);
            } catch (error) {
                console.error("Error fetching home rankings:", error);
                setRankingItems([]);
                setErrorMessage('랭킹 데이터를 불러오지 못했습니다.');
            } finally {
                setIsLoading(false);
            }
        };

        fetchRankings();
    }, [selectedRankingType, selectedAudience, selectedPriceRange]);

    return (
        <section className="main-pane-container">
            <div className="ranking-stage">
                <div className="ranking-header">
                    <div className="ranking-title-row">
                        <h2>실시간 선물랭킹</h2>
                        <button
                            type="button"
                            className="ranking-info-button"
                            aria-label="랭킹 안내 보기"
                            onClick={() => setShowRankingGuide((prev) => !prev)}
                        >
                            <i className="fa-solid fa-circle-info" />
                        </button>
                    </div>
                    {showRankingGuide && (
                        <div className="ranking-guide-popover">
                            <div className="ranking-guide-title">랭킹 노출 기준</div>
                            <div className="ranking-guide-item">
                                <strong>많이 펀딩한</strong>
                                <p>펀딩에 등록된 횟수를 기준으로 집계합니다.</p>
                            </div>
                            <div className="ranking-guide-item">
                                <strong>많이 구매한</strong>
                                <p>실제 주문 수량을 기준으로 집계합니다.</p>
                            </div>
                            <div className="ranking-guide-item">
                                <strong>많이 위시에 담은</strong>
                                <p>위시리스트에 담긴 횟수를 기준으로 집계합니다.</p>
                            </div>
                        </div>
                    )}
                </div>

                <div className="audience-selector">
                    {AUDIENCE_TYPES.map((option) => (
                        <button
                            key={option.value}
                            type="button"
                            className={`audience-option ${selectedAudience === option.value ? 'active' : ''}`}
                            onClick={() => setSelectedAudience(option.value)}
                        >
                            <span className="audience-icon">
                                {option.iconClass ? <i className={option.iconClass} /> : option.badge}
                            </span>
                            <span className="audience-label">{option.label}</span>
                        </button>
                    ))}
                </div>

                <div className="ranking-type-strip">
                    {RANKING_TYPES.map((option) => (
                        <button
                            key={option.value}
                            type="button"
                            className={`ranking-type-tab ${selectedRankingType === option.value ? 'active' : ''}`}
                            onClick={() => setSelectedRankingType(option.value)}
                        >
                            {option.label}
                        </button>
                    ))}
                </div>

                <div className="price-range-strip">
                    {PRICE_RANGES.map((option) => (
                        <button
                            key={option.value}
                            type="button"
                            className={`price-range-chip ${selectedPriceRange === option.value ? 'active' : ''}`}
                            onClick={() => setSelectedPriceRange(option.value)}
                        >
                            {option.label}
                        </button>
                    ))}
                </div>
            </div>

            <div className="ranking-item-grid">
                {rankingItems.length > 0 ? (
                    rankingItems.map((product) => (
                        <div className="ranking-grid-item" key={product.itemId}>
                            <MainRankingItem product={product} metricType={selectedRankingType} />
                        </div>
                    ))
                ) : isLoading ? (
                    <div className="ranking-empty-state">랭킹을 계산하는 중입니다.</div>
                ) : errorMessage ? (
                    <div className="ranking-empty-state">{errorMessage}</div>
                ) : (
                    <div className="ranking-empty-state">아직 집계된 랭킹 데이터가 없습니다.</div>
                )}
            </div>
        </section>
    );
};

export default MainPane;
