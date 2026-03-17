import React from 'react';
import './mainFunding.scss';
import MemberYesFunding from "../mainMyfunding/memberYesFunding/memberYesFunding";
import MemberNoFunding from "../mainMyfunding/memberNoFunding/memberNoFunding";
import MainFriendFunding from "../mainFriendFunding/mainFriendFunding/mainFriendFunding";
import NonMember from "../mainMyfunding/nonMember/nonMember";
import MainFriendNoFunding from "../mainFriendFunding/mainFriendNoFunding/mainFriendNoFunding";
import { useRecoilValue } from 'recoil';
import { loginState } from '../../../state/auth';

const MainFunding = ({ mainData }) => {
    const login = useRecoilValue(loginState);

    return (
        <div>
            {/* 로그인 상태에 따라 다른 컴포넌트 렌더링 */}
            {login.isAuthenticated ? (
                // 로그인 상태일 때
                <>
                    {/* my 펀딩 존재 여부에 따른 변화 */}
                    {mainData.data?.homeMyFundingStatusDto ? (
                        <MemberYesFunding memberFundingData={mainData} />
                    ) : (
                        <MemberNoFunding memberFundingData={mainData} />
                    )}

                    {/* 친구 펀딩 존재 여부에 따른 변화 */}
                    {mainData.data?.homeFriendFundingDtoList?.length > 0 ? (
                        <MainFriendFunding memberFundingData={mainData} />
                    ) : (
                        <MainFriendNoFunding />
                    )}
                </>
            ) : (
                // 로그인 상태가 아닐 때
                <NonMember />
            )}
        </div>
    );
};

export default MainFunding;
