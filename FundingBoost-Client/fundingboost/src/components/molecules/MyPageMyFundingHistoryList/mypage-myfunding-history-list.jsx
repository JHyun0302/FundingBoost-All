import React, { useEffect, useMemo, useState } from 'react';
import "./mypage-myfunding-history-list.scss";
import MyPageMyFundingHistory from "../../atoms/MyPageMyFundingHistory/mypage-myfunding-history";
import axios from 'axios';
import MyPageFundingHistoryDetailModal from "../MyPageFundingHistoryDetailModal/mypage-funding-history-detail-modal";

const ITEMS_PER_PAGE = 10;

export default function MyPageMyFundingHistoryList({ apiData }) {
    const fundingDetailHistoryDtos = apiData ? apiData.myPageFundingDetailHistoryDtos : [];
    const [detailData, setDetailData] = useState(null);
    const [isDetailLoading, setIsDetailLoading] = useState(false);
    const [detailError, setDetailError] = useState('');
    const [currentPage, setCurrentPage] = useState(1);

    const totalPages = Math.max(1, Math.ceil(fundingDetailHistoryDtos.length / ITEMS_PER_PAGE));
    const pagedFundingDetailHistoryDtos = useMemo(() => {
        const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        return fundingDetailHistoryDtos.slice(startIndex, startIndex + ITEMS_PER_PAGE);
    }, [currentPage, fundingDetailHistoryDtos]);

    useEffect(() => {
        setCurrentPage(1);
    }, [fundingDetailHistoryDtos.length]);

    const openFundingDetail = async (fundingId) => {
        try {
            const accessToken = localStorage.getItem('accessToken');
            if (!accessToken) {
                return;
            }

            setIsDetailLoading(true);
            setDetailError('');

            const response = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/funding/history/${fundingId}`, {
                headers: {
                    Authorization: `Bearer ${accessToken}`,
                },
                responseType: 'json',
            });

            setDetailData(response.data.data);
        } catch (error) {
            console.error('펀딩 이력 상세 조회 실패:', error);
            setDetailError('상세 정보를 불러오지 못했습니다.');
        } finally {
            setIsDetailLoading(false);
        }
    };

    return (
        <div className="MyPageFundingRecord">
            <div className='mypage-myfunding-history-title'>
                <div className="mypage-fundingRecordTitle">지난 펀딩 이력</div>
                <div className='historyGuideText'>펀딩 카드를 눌러 상세를 확인하세요.</div>
            </div>
            <div className="mypage-FH-horizontalLine"></div>
            <div className="fundingRecordItem">
                {pagedFundingDetailHistoryDtos.map((dto) => (
                    <MyPageMyFundingHistory key={dto.fundingId} data={dto} onOpenDetail={openFundingDetail} />
                ))}
                {fundingDetailHistoryDtos.length === 0 && (
                    <div className="fundingHistoryEmptyState">지난 펀딩 이력이 아직 없습니다.</div>
                )}
            </div>
            {fundingDetailHistoryDtos.length > ITEMS_PER_PAGE && (
                <div className="fundingHistoryPagination">
                    <button
                        type="button"
                        className="fundingHistoryPageButton"
                        onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
                        disabled={currentPage === 1}
                    >
                        이전
                    </button>
                    <div className="fundingHistoryPageNumbers">
                        {Array.from({ length: totalPages }, (_, index) => {
                            const pageNumber = index + 1;
                            return (
                                <button
                                    type="button"
                                    key={pageNumber}
                                    className={`fundingHistoryPageButton ${currentPage === pageNumber ? 'active' : ''}`}
                                    onClick={() => setCurrentPage(pageNumber)}
                                >
                                    {pageNumber}
                                </button>
                            );
                        })}
                    </div>
                    <button
                        type="button"
                        className="fundingHistoryPageButton"
                        onClick={() => setCurrentPage((prev) => Math.min(prev + 1, totalPages))}
                        disabled={currentPage === totalPages}
                    >
                        다음
                    </button>
                </div>
            )}
            {isDetailLoading && <div className="fundingHistoryDetailLoading">상세 정보를 불러오는 중입니다...</div>}
            {detailError && <div className="fundingHistoryDetailError">{detailError}</div>}
            {detailData && (
                <MyPageFundingHistoryDetailModal
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
