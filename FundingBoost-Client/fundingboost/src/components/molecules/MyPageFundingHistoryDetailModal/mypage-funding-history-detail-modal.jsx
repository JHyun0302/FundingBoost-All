import React from 'react';
import './mypage-funding-history-detail-modal.scss';
import defaultProfile from '../../../assets/defaultProfile.svg';
import { useNavigate } from "react-router-dom";
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

const MyPageFundingHistoryDetailModal = ({ detailData, onClose }) => {
    const navigate = useNavigate();

    if (!detailData) {
        return null;
    }

    const items = Array.isArray(detailData.myPageFundingItemDtoList) ? detailData.myPageFundingItemDtoList : [];
    const contributors = Array.isArray(detailData.participateFriendDtoList) ? detailData.participateFriendDtoList : [];

    return (
        <div className="myPageFundingHistoryDetailModal">
            <button type="button" className="myPageFundingHistoryDetailBackdrop" onClick={onClose} aria-label="상세 보기 닫기" />
            <div className="myPageFundingHistoryDetailPanel" role="dialog" aria-modal="true">
                <div className="myPageFundingHistoryDetailHeader">
                    <div>
                        <div className="myPageFundingHistoryDetailEyebrow">지난 펀딩 상세</div>
                        <div className="myPageFundingHistoryDetailTitle">
                            {detailData.createdDate} ~ {detailData.deadline}
                        </div>
                    </div>
                    <div className="myPageFundingHistoryDetailHeaderRight">
                        <div className="myPageFundingHistoryDetailPercent">{detailData.totalPercent}% 달성</div>
                        <button type="button" className="myPageFundingHistoryDetailClose" onClick={onClose}>닫기</button>
                    </div>
                </div>

                <div className="myPageFundingHistoryDetailBody">
                    <div className="myPageFundingHistoryDetailSection">
                        <div className="myPageFundingHistoryDetailSectionTitle">아이템 상세</div>
                        <div className="myPageFundingHistoryDetailItemGrid">
                            {items.map((item) => (
                                <button
                                    type="button"
                                    className="myPageFundingHistoryDetailItemCard"
                                    key={item.fundingItemId}
                                    onClick={() => navigate(`/shopping/detail/${item.itemId}`)}
                                >
                                    <img
                                        src={toImageProxyUrl(item.itemImageUrl)}
                                        alt={item.itemName}
                                        className="myPageFundingHistoryDetailItemImage"
                                    />
                                    <div className="myPageFundingHistoryDetailItemText">
                                        <div className="myPageFundingHistoryDetailItemName">{item.itemName}</div>
                                        {item.optionName && (
                                            <div className="myPageFundingHistoryDetailItemOption">{item.optionName}</div>
                                        )}
                                        <div className="myPageFundingHistoryDetailItemPrice">{item.itemPrice.toLocaleString()}원</div>
                                        <div className="myPageFundingHistoryDetailItemMeta">
                                            진행률 {item.itemPercent}% · {item.finishedStatus ? '정산 전' : '정산 완료'}
                                        </div>
                                        <div className="myPageFundingHistoryDetailItemLink">상품 상세 보기</div>
                                    </div>
                                </button>
                            ))}
                        </div>
                    </div>

                    <div className="myPageFundingHistoryDetailSection">
                        <div className="myPageFundingHistoryDetailSectionTitle">참여 친구</div>
                        <div className="myPageFundingHistoryDetailContributorList">
                            {contributors.map((friend, index) => (
                                <div className="myPageFundingHistoryDetailContributorCard" key={`${friend.participateNickname}-${index}`}>
                                    <img
                                        src={toImageProxyUrl(friend.participateProfileImgUrl || defaultProfile)}
                                        alt={friend.participateNickname}
                                        className="myPageFundingHistoryDetailContributorImage"
                                    />
                                    <div className="myPageFundingHistoryDetailContributorText">
                                        <div className="myPageFundingHistoryDetailContributorName">{friend.participateNickname}</div>
                                        <div className="myPageFundingHistoryDetailContributorPrice">
                                            {friend.participatePrice.toLocaleString()}원 펀딩
                                        </div>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
};

export default MyPageFundingHistoryDetailModal;
