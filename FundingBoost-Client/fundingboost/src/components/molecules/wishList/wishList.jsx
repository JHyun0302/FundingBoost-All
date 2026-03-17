import React, { useEffect, useMemo, useState } from 'react';
import './wishList.scss';
import WishListSingleItem from "../wishList-SingleItem/wishList-SingleItem";

const ITEMS_PER_PAGE = 9;
const ALL_CATEGORY = "전체";
const UNKNOWN_CATEGORY = "미분류";

const WishList = ({wishListData, onRemoveBookmark, isRemoving}) => {
    const bookmarkItemDtos = wishListData?.bookmarkItemDtos ?? [];
    const [selectedCategory, setSelectedCategory] = useState(ALL_CATEGORY);
    const [currentPage, setCurrentPage] = useState(1);

    const normalizedBookmarkItemDtos = useMemo(() => {
        return bookmarkItemDtos.map((item) => {
            const rawCategory = item?.category ?? item?.itemCategory ?? item?.categoryName ?? item?.item_category ?? "";
            const normalizedCategory = (typeof rawCategory === "string" ? rawCategory : String(rawCategory)).trim() || UNKNOWN_CATEGORY;
            return {
                ...item,
                category: normalizedCategory
            };
        });
    }, [bookmarkItemDtos]);

    const categoryCounts = useMemo(() => {
        const counts = normalizedBookmarkItemDtos.reduce((acc, item) => {
            const key = item.category;
            acc[key] = (acc[key] ?? 0) + 1;
            return acc;
        }, {});

        return {
            ...counts,
            [ALL_CATEGORY]: normalizedBookmarkItemDtos.length
        };
    }, [normalizedBookmarkItemDtos]);

    const categories = useMemo(() => {
        const uniqueCategories = Array.from(
            new Set(normalizedBookmarkItemDtos.map((item) => item.category))
        );
        uniqueCategories.sort((left, right) => left.localeCompare(right, "ko"));
        return [ALL_CATEGORY, ...uniqueCategories];
    }, [normalizedBookmarkItemDtos]);

    const filteredBookmarkItemDtos = useMemo(() => {
        if (selectedCategory === ALL_CATEGORY) {
            return normalizedBookmarkItemDtos;
        }
        return normalizedBookmarkItemDtos.filter((item) => item.category === selectedCategory);
    }, [normalizedBookmarkItemDtos, selectedCategory]);

    const totalPages = Math.max(1, Math.ceil(filteredBookmarkItemDtos.length / ITEMS_PER_PAGE));
    const pagedBookmarkItemDtos = useMemo(() => {
        const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        return filteredBookmarkItemDtos.slice(startIndex, startIndex + ITEMS_PER_PAGE);
    }, [filteredBookmarkItemDtos, currentPage]);

    useEffect(() => {
        setCurrentPage(1);
    }, [selectedCategory]);

    useEffect(() => {
        if (!categories.includes(selectedCategory)) {
            setSelectedCategory(ALL_CATEGORY);
        }
    }, [categories, selectedCategory]);

    useEffect(() => {
        if (currentPage > totalPages) {
            setCurrentPage(totalPages);
        }
    }, [currentPage, totalPages]);

    return (
        <div className="wishList">
            <div className="wishList-container">
                <div className="wishList-box">
                    <div className="wish-list">
                        <div className="wish-list-head">
                            <div className="wish-list-wish-management">위시리스트</div>
                        </div>

                        <div className="wish-list-line"/>

                        {bookmarkItemDtos.length > 0 && (
                            <div className="wishListCategoryFilter" role="tablist" aria-label="위시리스트 카테고리">
                                {categories.map((category) => (
                                    <button
                                        type="button"
                                        key={category}
                                        className={`wishListCategoryButton ${selectedCategory === category ? "active" : ""}`}
                                        onClick={() => setSelectedCategory(category)}
                                        role="tab"
                                        aria-selected={selectedCategory === category}
                                    >
                                        <span>{category}</span>
                                        <strong>{categoryCounts[category] ?? 0}</strong>
                                    </button>
                                ))}
                            </div>
                        )}

                        <div className="wishList-SingleItem">
                            {pagedBookmarkItemDtos.map((wishListItemData) => (
                            <div key={wishListItemData.itemId} className="wish-list-single-item">
                                <WishListSingleItem
                                    wishListData={wishListItemData}
                                    onRemoveBookmark={onRemoveBookmark}
                                    isRemoving={isRemoving}
                                />
                            </div>
                                ))}

                        </div>
                        {filteredBookmarkItemDtos.length === 0 && (
                            <div className="wishList-empty-state">위시리스트에 담긴 상품이 없습니다.</div>
                        )}
                        {filteredBookmarkItemDtos.length > ITEMS_PER_PAGE && (
                            <div className="wishListPagination">
                                <button
                                    type="button"
                                    className="wishListPageButton"
                                    onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
                                    disabled={currentPage === 1}
                                >
                                    이전
                                </button>
                                <div className="wishListPageNumbers">
                                    {Array.from({ length: totalPages }, (_, index) => {
                                        const pageNumber = index + 1;
                                        return (
                                            <button
                                                type="button"
                                                key={pageNumber}
                                                className={`wishListPageButton ${currentPage === pageNumber ? 'active' : ''}`}
                                                onClick={() => setCurrentPage(pageNumber)}
                                            >
                                                {pageNumber}
                                            </button>
                                        );
                                    })}
                                </div>
                                <button
                                    type="button"
                                    className="wishListPageButton"
                                    onClick={() => setCurrentPage((prev) => Math.min(prev + 1, totalPages))}
                                    disabled={currentPage === totalPages}
                                >
                                    다음
                                </button>
                            </div>
                        )}
                    </div>

                </div>
            </div>
        </div>
    );
};

export default WishList;
