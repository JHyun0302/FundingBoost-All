import React from "react";
import "./mypage-friend-funding-item.scss";
import defaultProfileImg from "../../../assets/defaultProfile.svg";
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

export default function MyPageFriendFundingItem ({ data, onOpenDetail }) {
    const profileSrc = data?.friendProfileImg
        ? (data.friendProfileImg.startsWith('http') || data.friendProfileImg.startsWith('/')
            ? data.friendProfileImg
            : `/${data.friendProfileImg}`)
        : defaultProfileImg;

    return (
        <div className="MyPageFriendFundingOneBox">
            <div className="MyPageFriendFundingOneView">
                <div className="friendFundingPrimaryColumn">
                    <img
                        className="friendFundingProfileImage"
                        alt={`${data.nickname} 프로필`}
                        src={toImageProxyUrl(profileSrc)}
                        loading="lazy"
                        decoding="async"
                    />
                    <div className="friendFundingMetaColumn">
                        <div className="friendFundingDate">{data.latestCreatedDate}</div>
                        <div className="friendFundingNameRow">
                            <button type="button" className="friendFundingNameButton" onClick={onOpenDetail}>
                                {data.nickname}
                            </button>
                            <div className="friendFundingTag">{data.tagSummary || "펀딩 참여"}</div>
                        </div>
                        <div className="friendFundingSubText">내가 참여한 펀딩 {data.contributionCount}건</div>
                    </div>
                </div>
                <div className="friendFundingTotalPrice">{data.totalContributedPrice.toLocaleString()} 원</div>
            </div>
        </div>
    );
};
