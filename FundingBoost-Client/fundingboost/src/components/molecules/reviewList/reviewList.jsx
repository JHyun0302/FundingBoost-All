import React, { useEffect, useMemo, useState } from 'react';
import SingleReview from "../singleReview/singleReview";
import './reviewList.scss';

const ITEMS_PER_PAGE = 10;

const ReviewList = ({myReviewList}) => {
    const myReviewItemDtoList = myReviewList?.myReviewItemDtoList ?? [];
    const [currentPage, setCurrentPage] = useState(1);
    const [sortType, setSortType] = useState('latest');

    const sortedReviewItemDtoList = useMemo(() => {
        const nextReviews = [...myReviewItemDtoList];

        if (sortType === 'rating') {
            nextReviews.sort((left, right) => {
                if (right.rating !== left.rating) {
                    return right.rating - left.rating;
                }
                if (right.createdDate !== left.createdDate) {
                    return right.createdDate.localeCompare(left.createdDate);
                }
                return right.reviewId - left.reviewId;
            });
            return nextReviews;
        }

        nextReviews.sort((left, right) => {
            if (right.createdDate !== left.createdDate) {
                return right.createdDate.localeCompare(left.createdDate);
            }
            return right.reviewId - left.reviewId;
        });
        return nextReviews;
    }, [myReviewItemDtoList, sortType]);

    const totalPages = Math.max(1, Math.ceil(sortedReviewItemDtoList.length / ITEMS_PER_PAGE));
    const pagedReviewItemDtoList = useMemo(() => {
        const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        return sortedReviewItemDtoList.slice(startIndex, startIndex + ITEMS_PER_PAGE);
    }, [currentPage, sortedReviewItemDtoList]);

    useEffect(() => {
        setCurrentPage(1);
    }, [myReviewItemDtoList.length, sortType]);

    return (
        <div className="reviewList">
            <div className="reviewList-container">
                <div className="reviewList-box">
                    <div className="review-list">
                        <div className="review-list-head">
                            <div className="review-list-delivery-management">MY 리뷰</div>
                            <div className="reviewSortControls">
                                <button
                                    type="button"
                                    className={`reviewSortButton ${sortType === 'latest' ? 'active' : ''}`}
                                    onClick={() => setSortType('latest')}
                                >
                                    최신순
                                </button>
                                <button
                                    type="button"
                                    className={`reviewSortButton ${sortType === 'rating' ? 'active' : ''}`}
                                    onClick={() => setSortType('rating')}
                                >
                                    높은 평점순
                                </button>
                            </div>
                        </div>
                        <div className="review-list-line"/>
                        <div className="review-list-addresses">
                            {pagedReviewItemDtoList.map((reviewItemData) => (
                                <SingleReview key={reviewItemData.reviewId} reviewData={reviewItemData} />
                            ))}
                            {myReviewItemDtoList.length === 0 && (
                                <div className="review-empty-state">작성한 리뷰가 아직 없습니다.</div>
                            )}
                        </div>
                        {myReviewItemDtoList.length > ITEMS_PER_PAGE && (
                            <div className="reviewPagination">
                                <button
                                    type="button"
                                    className="reviewPageButton"
                                    onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
                                    disabled={currentPage === 1}
                                >
                                    이전
                                </button>
                                <div className="reviewPageNumbers">
                                    {Array.from({ length: totalPages }, (_, index) => {
                                        const pageNumber = index + 1;
                                        return (
                                            <button
                                                type="button"
                                                key={pageNumber}
                                                className={`reviewPageButton ${currentPage === pageNumber ? 'active' : ''}`}
                                                onClick={() => setCurrentPage(pageNumber)}
                                            >
                                                {pageNumber}
                                            </button>
                                        );
                                    })}
                                </div>
                                <button
                                    type="button"
                                    className="reviewPageButton"
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

export default ReviewList;
