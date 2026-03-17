import React from "react";
import "./order-history-detail-modal.scss";
import defaultProfileImg from "../../../assets/defaultProfile.svg";
import { useNavigate } from "react-router-dom";
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

export default function OrderHistoryDetailModal({ detailData, onClose }) {
    const navigate = useNavigate();

    return (
        <div className="orderHistoryDetailModal">
            <button type="button" className="orderHistoryDetailBackdrop" onClick={onClose} aria-label="구매 이력 상세 닫기" />
            <div className="orderHistoryDetailPanel" role="dialog" aria-modal="true">
                <div className="orderHistoryDetailHeader">
                    <div>
                        <div className="orderHistoryDetailEyebrow">구매 이력 상세</div>
                        <div className="orderHistoryDetailTitle">{detailData.itemName}</div>
                        <div className="orderHistoryDetailSubTitle">{detailData.createdDate} · {detailData.paymentLabel}</div>
                    </div>
                    <button type="button" className="orderHistoryDetailClose" onClick={onClose}>닫기</button>
                </div>

                <div className="orderHistoryDetailContent">
                    <div className="orderHistoryDetailItemCard">
                        <img
                            src={toImageProxyUrl(detailData.itemImageUrl)}
                            alt={detailData.itemName}
                            className="orderHistoryDetailItemImage"
                            loading="lazy"
                            decoding="async"
                        />
                        <div className="orderHistoryDetailItemText">
                            {detailData.optionName && <div className="orderHistoryDetailOption">{detailData.optionName}</div>}
                            <div className="orderHistoryDetailQuantity">수량 {detailData.quantity}개</div>
                            <div className="orderHistoryDetailPrice">총 결제 금액 {detailData.totalPrice.toLocaleString()}원</div>
                            <button
                                type="button"
                                className="orderHistoryDetailMoveButton"
                                onClick={() => navigate(`/shopping/detail/${detailData.itemId}`)}
                            >
                                상품 상세로 이동
                            </button>
                        </div>
                    </div>

                    <div className="orderHistoryDetailGrid">
                        <div className="orderHistoryDetailSection">
                            <div className="orderHistoryDetailSectionTitle">결제 정보</div>
                            <div className="orderHistoryDetailPaymentCard">
                                <div className="orderHistoryDetailPaymentRow">
                                    <span>직접 결제</span>
                                    <strong>{detailData.directPaidAmount.toLocaleString()}원</strong>
                                </div>
                                <div className="orderHistoryDetailPaymentRow">
                                    <span>포인트 사용</span>
                                    <strong>{detailData.pointUsedAmount.toLocaleString()}원</strong>
                                </div>
                                <div className="orderHistoryDetailPaymentRow">
                                    <span>펀딩으로 받은 금액</span>
                                    <strong>{detailData.fundingSupportedAmount.toLocaleString()}원</strong>
                                </div>
                            </div>
                        </div>

                        <div className="orderHistoryDetailSection">
                            <div className="orderHistoryDetailSectionTitle">배송 정보</div>
                            <div className="orderHistoryDetailDeliveryCard">
                                <div className="orderHistoryDetailDeliveryName">{detailData.customerName}</div>
                                <div className="orderHistoryDetailDeliveryText">
                                    우편번호 {detailData.postalCode || '미입력'}
                                </div>
                                <div className="orderHistoryDetailDeliveryText">{detailData.address}</div>
                                <div className="orderHistoryDetailDeliveryText">{detailData.phoneNumber}</div>
                                <div className="orderHistoryDetailDeliveryText">
                                    배송 메모 {detailData.deliveryMemo || '요청사항 없음'}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="orderHistoryDetailSection">
                        <div className="orderHistoryDetailSectionTitle">펀딩 참여 내역</div>
                        {detailData.contributors?.length > 0 ? (
                            <div className="orderHistoryDetailContributorList">
                                {detailData.contributors.map((contributor, index) => {
                                    const profileSrc = contributor.profileImgUrl
                                        ? (contributor.profileImgUrl.startsWith("http") || contributor.profileImgUrl.startsWith("/")
                                            ? contributor.profileImgUrl
                                            : `/${contributor.profileImgUrl}`)
                                        : defaultProfileImg;

                                    return (
                                        <div className="orderHistoryDetailContributorCard" key={`${contributor.nickname}-${index}`}>
                                            <img
                                                src={toImageProxyUrl(profileSrc)}
                                                alt={`${contributor.nickname} 프로필`}
                                                className="orderHistoryDetailContributorImage"
                                            />
                                            <div className="orderHistoryDetailContributorText">
                                                <div className="orderHistoryDetailContributorName">{contributor.nickname}</div>
                                                <div className="orderHistoryDetailContributorPrice">
                                                    {contributor.fundedPrice.toLocaleString()}원 후원
                                                </div>
                                            </div>
                                        </div>
                                    );
                                })}
                            </div>
                        ) : (
                            <div className="orderHistoryDetailEmptyFunding">이 주문은 펀딩 지원 없이 결제되었습니다.</div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
}
