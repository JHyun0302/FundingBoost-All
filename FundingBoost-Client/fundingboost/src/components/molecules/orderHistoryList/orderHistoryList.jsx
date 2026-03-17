import React, { useEffect, useMemo, useState } from 'react';
import OrderHistorySingleOrder from '../orderHistory-singleOrder/orderHistory-singleOrder';
import './orderHistoryList.scss';
import axios from "axios";
import OrderHistoryDetailModal from "../OrderHistoryDetailModal/order-history-detail-modal";

const ITEMS_PER_PAGE = 10;

const OrderHistoryList = ({orderHistoryData}) => {
    console.log("구매내역:",orderHistoryData);
    const orderHistoryItemDtoList = orderHistoryData?.orderHistoryItemDtoList ?? [];
    const [currentPage, setCurrentPage] = useState(1);
    const [detailData, setDetailData] = useState(null);
    const [detailError, setDetailError] = useState('');
    const [isDetailLoading, setIsDetailLoading] = useState(false);

    const totalPages = Math.max(1, Math.ceil(orderHistoryItemDtoList.length / ITEMS_PER_PAGE));
    const pagedOrderHistoryItemDtoList = useMemo(() => {
        const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        return orderHistoryItemDtoList.slice(startIndex, startIndex + ITEMS_PER_PAGE);
    }, [currentPage, orderHistoryItemDtoList]);

    useEffect(() => {
        setCurrentPage(1);
    }, [orderHistoryItemDtoList.length]);

    const openOrderDetail = async (orderItemId) => {
        try {
            const accessToken = localStorage.getItem('accessToken');
            if (!accessToken) {
                return;
            }

            setIsDetailLoading(true);
            setDetailError('');

            const response = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/order/history/${orderItemId}`, {
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                },
                responseType: 'json',
            });

            setDetailData(response.data.data);
        } catch (error) {
            console.error('구매 이력 상세 조회 실패:', error);
            setDetailError('상세 정보를 불러오지 못했습니다.');
        } finally {
            setIsDetailLoading(false);
        }
    };

    return (
        <div className="orderHistoryList">
            <div className="orderHistoryList-container">
                <div className="orderHistoryList-box">
                    <div className="order-history-list">
                        <div className="order-history-list-head">
                            <div className="order-history-list-delivery-management">구매 이력</div>
                            <div className="order-history-list-guide">아이템 카드를 눌러 결제·배송 정보를 확인하세요.</div>
                        </div>
                        <div className="order-history-list-line"/>

                        <div className="MyPageFriendFundingListOverlap-group">
                            <div className="MyPageFriendFundingListText-wrapper-7">ITEM</div>
                            <div className="MyPageFriendFundingListText-wrapper-6">구매한 금액</div>
                        </div>

                        {pagedOrderHistoryItemDtoList.map((orderHistoryListData) => (
                            <div key={orderHistoryListData.orderItemId} className="order-history-list-addresses" >
                                <OrderHistorySingleOrder
                                    orderHistoryData={orderHistoryListData}
                                    onOpenDetail={openOrderDetail}
                                />
                            </div>
                        ))}

                        {orderHistoryItemDtoList.length === 0 && (
                            <div className="order-history-empty-state">구매 이력이 아직 없습니다.</div>
                        )}

                        {orderHistoryItemDtoList.length > ITEMS_PER_PAGE && (
                            <div className="orderHistoryPagination">
                                <button
                                    type="button"
                                    className="orderHistoryPageButton"
                                    onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
                                    disabled={currentPage === 1}
                                >
                                    이전
                                </button>
                                <div className="orderHistoryPageNumbers">
                                    {Array.from({ length: totalPages }, (_, index) => {
                                        const pageNumber = index + 1;
                                        return (
                                            <button
                                                type="button"
                                                key={pageNumber}
                                                className={`orderHistoryPageButton ${currentPage === pageNumber ? 'active' : ''}`}
                                                onClick={() => setCurrentPage(pageNumber)}
                                            >
                                                {pageNumber}
                                            </button>
                                        );
                                    })}
                                </div>
                                <button
                                    type="button"
                                    className="orderHistoryPageButton"
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
            {isDetailLoading && <div className="order-history-message">상세 정보를 불러오는 중입니다...</div>}
            {detailError && <div className="order-history-message order-history-message-error">{detailError}</div>}
            {detailData && (
                <OrderHistoryDetailModal
                    detailData={detailData}
                    onClose={() => {
                        setDetailData(null);
                        setDetailError('');
                    }}
                />
            )}
        </div>
    );
};

export default OrderHistoryList;
