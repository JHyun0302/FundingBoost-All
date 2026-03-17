import React, { useEffect, useMemo, useState } from "react";
import "./mypage-friend-funding-list.scss";
import MyPageFriendFundingItem from "../../atoms/MyPageFriendFundingItem/mypage-friend-funding-item";
import MyPageFriendFundingDetailModal from "../MyPageFriendFundingDetailModal/mypage-friend-funding-detail-modal";

const ITEMS_PER_PAGE = 10;

export default function MyPageFriendFundingList ({ apiData }) {
    const friendFundingContributionDtos = apiData ? apiData.FriendFundingContributionDto : [];
    const [selectedFriendGroup, setSelectedFriendGroup] = useState(null);
    const [currentPage, setCurrentPage] = useState(1);

    const friendFundingGroups = useMemo(() => {
        const grouped = new Map();

        friendFundingContributionDtos.forEach((dto) => {
            const groupKey = dto.friendMemberId ?? dto.nickname;
            if (!grouped.has(groupKey)) {
                grouped.set(groupKey, {
                    friendMemberId: dto.friendMemberId,
                    nickname: dto.nickname,
                    friendProfileImg: dto.friendProfileImg,
                    latestCreatedDate: dto.createdDate,
                    totalContributedPrice: 0,
                    contributionCount: 0,
                    tags: new Set(),
                    contributions: [],
                });
            }

            const group = grouped.get(groupKey);
            group.totalContributedPrice += dto.price;
            group.contributionCount += 1;
            group.tags.add(dto.tag);
            if (dto.createdDate > group.latestCreatedDate) {
                group.latestCreatedDate = dto.createdDate;
            }
            group.contributions.push(dto);
        });

        return Array.from(grouped.values())
            .map((group) => ({
                ...group,
                tagSummary: Array.from(group.tags).slice(0, 2).join(" · "),
                contributions: group.contributions.sort((a, b) => b.createdDate.localeCompare(a.createdDate)),
            }))
            .sort((a, b) => b.latestCreatedDate.localeCompare(a.latestCreatedDate));
    }, [friendFundingContributionDtos]);

    const totalPages = Math.max(1, Math.ceil(friendFundingGroups.length / ITEMS_PER_PAGE));
    const pagedFriendFundingGroups = useMemo(() => {
        const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        return friendFundingGroups.slice(startIndex, startIndex + ITEMS_PER_PAGE);
    }, [currentPage, friendFundingGroups]);

    useEffect(() => {
        setCurrentPage(1);
    }, [friendFundingGroups.length]);

    return (
        <div className="MyPageFriendFundingListBox">
            <div className="MyPageFriendFundingListView">
                <div className="MyPageFriendFundingListTitle">
                    <div className="MyPageFriendFundingListText-wrapper">친구 펀딩 기록</div>
                    <div className="MyPageFriendFundingListText-wrapper-2">친구 이름을 눌러 참여 내역을 확인하세요.</div>
                </div>
                <div className="mypage-FH-horizontalLine"/>
                <div className="MyPageFriendFundingListOverlap-group">
                    <div className="MyPageFriendFundingListText-wrapper-7">TO</div>
                    <div className="MyPageFriendFundingListText-wrapper-6">내가 보낸 총 펀딩액</div>
                </div>
                <div className="MyPageFriendFundingListView-2">
                    {pagedFriendFundingGroups.map((group) => (
                        <MyPageFriendFundingItem
                            key={group.friendMemberId ?? group.nickname}
                            data={group}
                            onOpenDetail={() => setSelectedFriendGroup(group)}
                        />
                    ))}
                    {friendFundingGroups.length === 0 && (
                        <div className="friendFundingEmptyState">아직 참여한 친구 펀딩 기록이 없습니다.</div>
                    )}
                </div>
                {friendFundingGroups.length > ITEMS_PER_PAGE && (
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
                            {Array.from({ length: totalPages }, (_, index) => {
                                const pageNumber = index + 1;
                                return (
                                    <button
                                        type="button"
                                        key={pageNumber}
                                        className={`friendFundingPageButton ${currentPage === pageNumber ? 'active' : ''}`}
                                        onClick={() => setCurrentPage(pageNumber)}
                                    >
                                        {pageNumber}
                                    </button>
                                );
                            })}
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
                )}
            </div>
            {selectedFriendGroup && (
                <MyPageFriendFundingDetailModal
                    friendGroup={selectedFriendGroup}
                    onClose={() => setSelectedFriendGroup(null)}
                />
            )}
        </div>
    );
};
