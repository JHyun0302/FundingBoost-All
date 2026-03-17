import ProfileImg from "../../../atoms/ProfileImg/ProfileImg";
import StartFundingButton  from "../../../atoms/button/mainMyfuudingBtn/startFunding-btn";
import React from "react";
import './memberNoFunding.scss'

function MainMyFunding({memberFundingData}) {

    return (
        <div className="memberNoFunding">
            <div className="main-memberNoFunding">
                <div className="profile-img-wrapper">
                <ProfileImg className="memberNoFunding-Profile" memberFundingData={memberFundingData.data}></ProfileImg>
                </div>
                <div className="memberNoFunding-item">
                    <div className="memberNoFunding-text">
                        <div className="memberNoFunding-Nickname"><b>{memberFundingData?.data?.homeMemberInfoDto?.nickName}님</b></div>
                        <div>펀딩의 주인공이 되어보세요 🎠</div>
                    </div>
                    <StartFundingButton className={"nonMember-StartFundingBtn"} />
                </div>
            </div>
        </div>
    );
}
export default MainMyFunding;