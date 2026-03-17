import React from "react";
import "./mypage-friend-funding-detail-modal.scss";
import defaultProfileImg from "../../../assets/defaultProfile.svg";
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

export default function MyPageFriendFundingDetailModal({ friendGroup, onClose }) {
    const profileSrc = friendGroup?.friendProfileImg
        ? (friendGroup.friendProfileImg.startsWith("http") || friendGroup.friendProfileImg.startsWith("/")
            ? friendGroup.friendProfileImg
            : `/${friendGroup.friendProfileImg}`)
        : defaultProfileImg;

    return (
        <div className="myPageFriendFundingDetailModal">
            <button
                type="button"
                className="myPageFriendFundingDetailBackdrop"
                onClick={onClose}
                aria-label="친구 펀딩 상세 닫기"
            />
            <div className="myPageFriendFundingDetailPanel" role="dialog" aria-modal="true">
                <div className="myPageFriendFundingDetailHeader">
                    <div className="myPageFriendFundingDetailTitleWrap">
                        <div className="myPageFriendFundingDetailEyebrow">친구 펀딩 상세</div>
                        <div className="myPageFriendFundingDetailTitle">{friendGroup.nickname}님에게 보낸 펀딩</div>
                        <div className="myPageFriendFundingDetailSubTitle">
                            총 {friendGroup.contributionCount}건 · 합계 {friendGroup.totalContributedPrice.toLocaleString()}원
                        </div>
                    </div>
                    <button type="button" className="myPageFriendFundingDetailClose" onClick={onClose}>닫기</button>
                </div>

                <div className="myPageFriendFundingDetailFriendCard">
                    <img
                        src={toImageProxyUrl(profileSrc)}
                        alt={`${friendGroup.nickname} 프로필`}
                        className="myPageFriendFundingDetailFriendImage"
                    />
                    <div className="myPageFriendFundingDetailFriendText">
                        <div className="myPageFriendFundingDetailFriendName">{friendGroup.nickname}</div>
                        <div className="myPageFriendFundingDetailFriendMeta">
                            최근 참여일 {friendGroup.latestCreatedDate}
                        </div>
                    </div>
                </div>

                <div className="myPageFriendFundingDetailList">
                    {friendGroup.contributions.map((contribution) => (
                        <div className="myPageFriendFundingDetailItemCard" key={`${contribution.fundingId}-${contribution.createdDate}`}>
                            <img
                                src={toImageProxyUrl(contribution.itemImageUrl)}
                                alt={contribution.itemName}
                                className="myPageFriendFundingDetailItemImage"
                                loading="lazy"
                                decoding="async"
                            />
                            <div className="myPageFriendFundingDetailItemText">
                                <div className="myPageFriendFundingDetailItemTop">
                                    <span className="myPageFriendFundingDetailTag">{contribution.tag}</span>
                                    <span className="myPageFriendFundingDetailDate">{contribution.createdDate}</span>
                                </div>
                                <div className="myPageFriendFundingDetailItemName">{contribution.itemName}</div>
                                {contribution.optionName && (
                                    <div className="myPageFriendFundingDetailItemOption">{contribution.optionName}</div>
                                )}
                                <div className="myPageFriendFundingDetailItemBottom">
                                    <div className="myPageFriendFundingDetailItemPrice">
                                        상품가 {contribution.itemPrice.toLocaleString()}원
                                    </div>
                                    <div className="myPageFriendFundingDetailContributionPrice">
                                        내가 보낸 금액 {contribution.price.toLocaleString()}원
                                    </div>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
}
