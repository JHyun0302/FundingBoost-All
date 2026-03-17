import React from 'react';
import "./mypageprofile.scss";
import defaultProfile from "../../../assets/defaultProfile.svg";
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

export default function MyPageProfile ({ profileInfo }) {
    const member = profileInfo?.myPageMemberDto;
    const isLoadingProfile = !member;
    const nickName = member?.nickName || '불러오는 중';
    const email = member?.email || '프로필 정보를 불러오는 중입니다.';
    const point = member?.point ? member.point.toLocaleString() : '0';

    return (
        <div className={`myPageProfileView ${isLoadingProfile ? 'is-loading' : ''}`}>
            <div className='myPageProfile'>
                <img
                    className="myPageProfileImg"
                    alt="myPageProfileImg"
                    src={toImageProxyUrl(member?.profileImgUrl || defaultProfile)}
                />
                <div className='myPageProfileText'>
                    <div className="myPageProfileName">{nickName}</div>
                    <p className="myPageProfileEmail">
                        {email}
                    </p>
                </div>
            </div>
            <div className="myPointExplain">내 포인트</div>
            <div className="horizontalLine"></div>
            <div className="myPoint">{point} P</div>
            <div className="horizontalLine"></div>
        </div>
    );
};
