import React, {useEffect, useMemo, useState} from 'react';
import './friendFunding-page.scss';
import Header from "../../organisms/header/header";
import Footer from "../../organisms/footer/footer";
import SingleFriendFunding from "../../molecules/Single-friendFunding/single-friendFunding";
import FriendFundingDropdownBtn from "../../atoms/friendFunding-DropdownBtn/friendFunding-DropdownBtn";
import axios from "axios";
import FriendNonFunding from "../../organisms/contents/FriendNonFunding/FriendNonFunding";
import {Link} from "react-router-dom";

import NonMemberModal from "../../atoms/nonMemberModal/nonMemberModal";

const FRIEND_FUNDING_ITEMS_PER_PAGE = 12;
const MAX_PAGE_BUTTONS = 7;
const DEADLINE_SOON_THRESHOLD = 3;
const TAG_ALL = '전체 태그';

const STATUS_OPTIONS = [
    {key: 'ALL', label: '전체'},
    {key: 'DEADLINE_SOON', label: '마감 임박'},
    {key: 'ALMOST_FUNDED', label: '달성 임박'},
    {key: 'FUNDED', label: '달성 완료'}
];

const SORT_OPTIONS = [
    '최신 등록순',
    '이름순',
    '마감 임박순',
    '달성률 높은순',
    '모금액 높은순'
];

const parseDeadlineRank = (deadlineText) => {
    if (typeof deadlineText !== 'string') {
        return Number.MAX_SAFE_INTEGER;
    }

    const normalizedText = deadlineText.trim();
    if (!normalizedText) {
        return Number.MAX_SAFE_INTEGER;
    }

    const numericMatch = normalizedText.match(/^D-(\d+)$/i);
    if (numericMatch) {
        return Number.parseInt(numericMatch[1], 10);
    }

    if (/D[- ]?DAY|오늘|금일/i.test(normalizedText)) {
        return 0;
    }

    return Number.MAX_SAFE_INTEGER;
};

const normalizeTag = (tag) => {
    if (typeof tag !== 'string') {
        return '#기타';
    }
    const normalizedTag = tag.trim();
    if (!normalizedTag) {
        return '#기타';
    }
    return normalizedTag.startsWith('#') ? normalizedTag : `#${normalizedTag}`;
};

const toSafeNumber = (value) => {
    const numericValue = Number(value);
    return Number.isFinite(numericValue) ? numericValue : 0;
};

const formatWon = (value) => `${toSafeNumber(value).toLocaleString()}원`;

const FriendFundingPage = () => {
    const [modalShowState, setModalShowState] = useState(false);
    const [friendFundingData, setFriendFundingData] = useState({ data: [] });
    const [sortOption, setSortOption] = useState(SORT_OPTIONS[0]);
    const [currentPage, setCurrentPage] = useState(1);
    const [searchKeyword, setSearchKeyword] = useState('');
    const [statusFilter, setStatusFilter] = useState('ALL');
    const [selectedTag, setSelectedTag] = useState(TAG_ALL);

    const normalizedFriendFundingItems = useMemo(() => {
        const rawItems = Array.isArray(friendFundingData?.data) ? friendFundingData.data : [];
        return rawItems.map((item) => {
            const deadlineRank = parseDeadlineRank(item?.friendFundingDeadlineDate);
            const normalizedTag = normalizeTag(item?.tag);
            const fundingPercent = Math.max(0, toSafeNumber(item?.friendFundingPercent));
            const collectPrice = Math.max(0, toSafeNumber(item?.collectPrice));

            let badgeLabel = '인기';
            let badgeTone = 'hot';
            if (fundingPercent >= 100) {
                badgeLabel = '달성 완료';
                badgeTone = 'success';
            } else if (deadlineRank <= DEADLINE_SOON_THRESHOLD) {
                badgeLabel = '마감 임박';
                badgeTone = 'soon';
            } else if (fundingPercent >= 80) {
                badgeLabel = '달성 임박';
                badgeTone = 'almost';
            }

            return {
                ...item,
                tag: normalizedTag,
                deadlineRank,
                friendFundingPercent: fundingPercent,
                collectPrice,
                badgeLabel,
                badgeTone,
                searchIndex: `${item?.nickName || ''} ${normalizedTag} ${item?.friendFundingDeadlineDate || ''}`.toLowerCase()
            };
        });
    }, [friendFundingData]);

    const tagOptions = useMemo(() => {
        const uniqueTags = Array.from(new Set(normalizedFriendFundingItems.map((item) => item.tag)));
        uniqueTags.sort((left, right) => left.localeCompare(right, 'ko'));
        return [TAG_ALL, ...uniqueTags];
    }, [normalizedFriendFundingItems]);

    const filteredFriendFundingItems = useMemo(() => {
        const normalizedKeyword = searchKeyword.trim().toLowerCase();
        return normalizedFriendFundingItems.filter((item) => {
            if (selectedTag !== TAG_ALL && item.tag !== selectedTag) {
                return false;
            }

            if (statusFilter === 'DEADLINE_SOON' && item.deadlineRank > DEADLINE_SOON_THRESHOLD) {
                return false;
            }
            if (statusFilter === 'ALMOST_FUNDED' && (item.friendFundingPercent < 80 || item.friendFundingPercent >= 100)) {
                return false;
            }
            if (statusFilter === 'FUNDED' && item.friendFundingPercent < 100) {
                return false;
            }

            if (normalizedKeyword && !item.searchIndex.includes(normalizedKeyword)) {
                return false;
            }
            return true;
        });
    }, [normalizedFriendFundingItems, searchKeyword, selectedTag, statusFilter]);

    const sortedFriendFundingItems = useMemo(() => {
        const rawItems = [...filteredFriendFundingItems];
        rawItems.sort((left, right) => {
            if (sortOption === '이름순') {
                return (left?.nickName || '').localeCompare(right?.nickName || '', 'ko');
            }

            if (sortOption === '마감 임박순') {
                if (left.deadlineRank !== right.deadlineRank) {
                    return left.deadlineRank - right.deadlineRank;
                }
                return right.friendFundingPercent - left.friendFundingPercent;
            }

            if (sortOption === '달성률 높은순') {
                if (left.friendFundingPercent !== right.friendFundingPercent) {
                    return right.friendFundingPercent - left.friendFundingPercent;
                }
                return right.collectPrice - left.collectPrice;
            }

            if (sortOption === '모금액 높은순') {
                if (left.collectPrice !== right.collectPrice) {
                    return right.collectPrice - left.collectPrice;
                }
                return right.friendFundingPercent - left.friendFundingPercent;
            }

            return (right?.fundingId || 0) - (left?.fundingId || 0);
        });
        return rawItems;
    }, [filteredFriendFundingItems, sortOption]);

    const highlightedRankingItems = useMemo(() => {
        return [...normalizedFriendFundingItems]
            .sort((left, right) => {
                if (left.friendFundingPercent !== right.friendFundingPercent) {
                    return right.friendFundingPercent - left.friendFundingPercent;
                }
                if (left.collectPrice !== right.collectPrice) {
                    return right.collectPrice - left.collectPrice;
                }
                return left.deadlineRank - right.deadlineRank;
            })
            .slice(0, 3);
    }, [normalizedFriendFundingItems]);

    const insightMetrics = useMemo(() => {
        const totalFundingCount = normalizedFriendFundingItems.length;
        const deadlineSoonCount = normalizedFriendFundingItems.filter((item) => item.deadlineRank <= DEADLINE_SOON_THRESHOLD).length;
        const averagePercent = totalFundingCount > 0
            ? Math.round(normalizedFriendFundingItems.reduce((sum, item) => sum + item.friendFundingPercent, 0) / totalFundingCount)
            : 0;
        const almostFundedCount = normalizedFriendFundingItems.filter((item) => item.friendFundingPercent >= 80 && item.friendFundingPercent < 100).length;

        return [
            {title: '진행 중 펀딩', value: `${totalFundingCount}개`, subtitle: '친구가 현재 진행 중인 프로젝트'},
            {title: '마감 임박', value: `${deadlineSoonCount}개`, subtitle: `D-${DEADLINE_SOON_THRESHOLD} 이내 종료 예정`},
            {title: '달성 임박', value: `${almostFundedCount}개`, subtitle: '달성률 80% 이상 펀딩'},
            {title: '평균 달성률', value: `${averagePercent}%`, subtitle: '전체 친구 펀딩 진행률 평균'}
        ];
    }, [normalizedFriendFundingItems]);

    const totalPages = Math.max(1, Math.ceil(sortedFriendFundingItems.length / FRIEND_FUNDING_ITEMS_PER_PAGE));

    const pagedFriendFundingData = useMemo(() => {
        const startIndex = (currentPage - 1) * FRIEND_FUNDING_ITEMS_PER_PAGE;
        const currentItems = sortedFriendFundingItems.slice(startIndex, startIndex + FRIEND_FUNDING_ITEMS_PER_PAGE);
        return {
            ...friendFundingData,
            data: currentItems
        };
    }, [currentPage, sortedFriendFundingItems, friendFundingData]);

    const visiblePageNumbers = useMemo(() => {
        if (totalPages <= MAX_PAGE_BUTTONS) {
            return Array.from({ length: totalPages }, (_, index) => index + 1);
        }

        const half = Math.floor(MAX_PAGE_BUTTONS / 2);
        let start = Math.max(currentPage - half, 1);
        let end = start + MAX_PAGE_BUTTONS - 1;
        if (end > totalPages) {
            end = totalPages;
            start = end - MAX_PAGE_BUTTONS + 1;
        }
        return Array.from({ length: end - start + 1 }, (_, index) => start + index);
    }, [currentPage, totalPages]);

    useEffect(() => {
        setCurrentPage(1);
    }, [sortOption, searchKeyword, statusFilter, selectedTag]);

    useEffect(() => {
        if (currentPage > totalPages) {
            setCurrentPage(totalPages);
        }
    }, [currentPage, totalPages]);

    useEffect(() => {
        if (!tagOptions.includes(selectedTag)) {
            setSelectedTag(TAG_ALL);
        }
    }, [tagOptions, selectedTag]);

    useEffect(() => {
        const fetchData = async () => {
            try {

                const accessToken = localStorage.getItem('accessToken');
                if (!accessToken) {
                    setModalShowState(true);
                    return;
                }

                const response = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/funding/friends`, {

                    responseType: 'json',
                    headers: {
                        "Content-Type": "application/json",
                        "Authorization": `Bearer ${accessToken}`,
                    },
                });
                setFriendFundingData(response.data);
                console.log("response ->", response.data);
            } catch (error) {
                console.error("Error data:", error);
            }
        };
        fetchData();
    },[] );

    return (
        <div>
            <Header />
            {modalShowState && <NonMemberModal message="로그인 후 친구들의 펀딩을 구경해보세요."/>}
            <FriendFundingDropdownBtn
                sortOption={sortOption}
                onSortChange={setSortOption}
            />
            {normalizedFriendFundingItems.length > 0 ? (
                <>
                    <section className="friendFundingInsightSection">
                        <div className="friendFundingInsightGrid">
                            {insightMetrics.map((metric) => (
                                <article key={metric.title} className="friendFundingInsightCard">
                                    <p className="friendFundingInsightTitle">{metric.title}</p>
                                    <strong className="friendFundingInsightValue">{metric.value}</strong>
                                    <p className="friendFundingInsightSubTitle">{metric.subtitle}</p>
                                </article>
                            ))}
                        </div>
                    </section>

                    <section className="friendFundingControlPanel">
                        <div className="friendFundingSearchRow">
                            <label className="friendFundingSearchLabel" htmlFor="friendFundingSearch">
                                친구/태그 검색
                            </label>
                            <input
                                id="friendFundingSearch"
                                type="text"
                                value={searchKeyword}
                                placeholder="친구 이름, 태그로 찾기"
                                onChange={(event) => setSearchKeyword(event.target.value)}
                            />
                        </div>

                        <div className="friendFundingFilterRow">
                            <div className="friendFundingFilterGroup">
                                <span className="friendFundingFilterLabel">상태</span>
                                <div className="friendFundingFilterChips">
                                    {STATUS_OPTIONS.map((option) => (
                                        <button
                                            key={option.key}
                                            type="button"
                                            className={`friendFundingFilterChip ${statusFilter === option.key ? 'active' : ''}`}
                                            onClick={() => setStatusFilter(option.key)}
                                        >
                                            {option.label}
                                        </button>
                                    ))}
                                </div>
                            </div>

                            <div className="friendFundingFilterGroup">
                                <span className="friendFundingFilterLabel">태그</span>
                                <div className="friendFundingFilterChips">
                                    {tagOptions.map((tagOption) => (
                                        <button
                                            key={tagOption}
                                            type="button"
                                            className={`friendFundingFilterChip ${selectedTag === tagOption ? 'active' : ''}`}
                                            onClick={() => setSelectedTag(tagOption)}
                                        >
                                            {tagOption}
                                        </button>
                                    ))}
                                </div>
                            </div>
                        </div>
                    </section>

                    {highlightedRankingItems.length > 0 && (
                        <section className="friendFundingRankingSection">
                            <div className="friendFundingRankingHeader">
                                <h3>친구 펀딩 실시간 랭킹</h3>
                                <p>달성률과 모금액 기준으로 지금 주목할 프로젝트를 추천합니다.</p>
                            </div>
                            <div className="friendFundingRankingList">
                                {highlightedRankingItems.map((item, index) => (
                                    <Link
                                        key={item.fundingId}
                                        className="friendFundingRankingCard"
                                        to={`/friend-funding/detail/${item.fundingId}`}
                                    >
                                        <div className="friendFundingRankingCardHeader">
                                            <span className="friendFundingRankingOrder">#{index + 1}</span>
                                            <span className={`friendFundingRankingBadge ${item.badgeTone}`}>{item.badgeLabel}</span>
                                        </div>
                                        <strong className="friendFundingRankingName">{item.nickName}</strong>
                                        <p className="friendFundingRankingMeta">{item.tag} · {item.friendFundingDeadlineDate}</p>
                                        <div className="friendFundingRankingProgressBar">
                                            <div style={{width: `${Math.min(item.friendFundingPercent, 100)}%`}} />
                                        </div>
                                        <p className="friendFundingRankingProgressText">
                                            {item.friendFundingPercent}% · {formatWon(item.collectPrice)}
                                        </p>
                                    </Link>
                                ))}
                            </div>
                        </section>
                    )}

                    {sortedFriendFundingItems.length > 0 ? (
                        <>
                            <SingleFriendFunding friendFundingData={pagedFriendFundingData} />
                            {totalPages > 1 && (
                                <div className="friendFundingPaginationWrap">
                                    <div className="friendFundingPaginationInfo">
                                        {currentPage} / {totalPages}
                                    </div>
                                    <div className="friendFundingPagination">
                                        <button
                                            type="button"
                                            className="friendFundingPageButton"
                                            onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
                                            disabled={currentPage === 1}
                                        >
                                            이전
                                        </button>
                                        <div className="friendFundingPageNumbers">
                                            {visiblePageNumbers.map((pageNumber) => (
                                                <button
                                                    key={pageNumber}
                                                    type="button"
                                                    className={`friendFundingPageButton ${currentPage === pageNumber ? 'active' : ''}`}
                                                    onClick={() => setCurrentPage(pageNumber)}
                                                >
                                                    {pageNumber}
                                                </button>
                                            ))}
                                        </div>
                                        <button
                                            type="button"
                                            className="friendFundingPageButton"
                                            onClick={() => setCurrentPage((prev) => Math.min(prev + 1, totalPages))}
                                            disabled={currentPage === totalPages}
                                        >
                                            다음
                                        </button>
                                    </div>
                                </div>
                            )}
                        </>
                    ) : (
                        <div className="friendFundingFilteredEmpty">
                            <p>선택한 조건에 맞는 친구 펀딩이 없습니다.</p>
                            <button
                                type="button"
                                onClick={() => {
                                    setSearchKeyword('');
                                    setStatusFilter('ALL');
                                    setSelectedTag(TAG_ALL);
                                }}
                            >
                                필터 초기화
                            </button>
                        </div>
                    )}
                </>
            ):(
            <FriendNonFunding />
            )}
            <Footer />
        </div>
    );
};

export default FriendFundingPage;
