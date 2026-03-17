import React from 'react';
import './singleReview.scss';
import { BsStarFill } from "react-icons/bs";
import { BsStarHalf } from "react-icons/bs";
import { BsStar } from "react-icons/bs";
import { useNavigate } from "react-router-dom";
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

const SingleReview = ({reviewData}) => {
    const navigate = useNavigate();

    const renderStars = () => {
        const stars = [];
        const fullStars = Math.floor(reviewData.rating);
        const hasHalfStar = reviewData.rating - fullStars >= 0.5;

        for (let index = 0; index < fullStars; index += 1) {
            stars.push(<BsStarFill key={`full-${index}`} />);
        }
        if (hasHalfStar) {
            stars.push(<BsStarHalf key="half" />);
        }
        while (stars.length < 5) {
            stars.push(<BsStar key={`empty-${stars.length}`} />);
        }

        return stars;
    };

    return (
        <button
            type="button"
            className="singleReview"
            onClick={() => navigate(`/shopping/detail/${reviewData.itemId}`)}
        >
            <div className="singleReview-header">
                <div className="singleReview-item">
                    <div className="review-item-img">
                        <img src={toImageProxyUrl(reviewData.itemImageUrl)} alt={reviewData.itemName} />
                    </div>
                    <div className="review-item">
                        <div className="review-item-name">{reviewData.itemName}</div>
                        <div className="review-item-text">
                            <div className="review-option">{reviewData.optionName || '옵션 없음'}</div>
                            <div className="review-price">{reviewData.itemPrice.toLocaleString()}원</div>
                        </div>
                    </div>
                </div>
                <hr style={{border: '0.05px solid', color: 'black', width: '100%',  margin: '0'}}/>
                <div className="review">
                    <div className="review-detail">
                        <div className="review-detail-info">
                            <p className="review-star">
                                {renderStars()}
                            </p>
                            <div className="review-day">{reviewData.createdDate}</div>

                            <div className="review-text">{reviewData.content}</div>
                        </div>

                        <div className="review-cta">상품 보러가기</div>
                    </div>

                </div>
            </div>
        </button>
    );
};

export default SingleReview;
