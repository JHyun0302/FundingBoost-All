import React from 'react';
import { Link } from 'react-router-dom';
import './mainFriendFunding.scss';
import ItemImg from "../../../atoms/itemImg/itemImg";
import GaugeBar from "../../../atoms/gauge-bar/gauge-bar";
import defaultProfileImg from "../../../../assets/defaultProfile.svg";
import { useEffect, useState } from "react";
import { Carousel } from 'react-responsive-carousel';
import { toImageProxyUrl } from "../../../../utils/imageProxyUrl";

function chunkArray(arr, chunkSize) {
    const chunkedArray = [];
    for (let i = 0; i < arr.length; i += chunkSize) {
        chunkedArray.push(arr.slice(i, i + chunkSize));
    }
    return chunkedArray;
}

const MainFriendFunding = ({ memberFundingData }) => {
    const [itemsSlide, setItemsSlide] = useState(2);

    console.log(memberFundingData)

    useEffect(() => {
        //반응형 추가
        function handleResize() {
            if (window.innerWidth >= 1411) setItemsSlide(5);
            else if (window.innerWidth >= 1130) setItemsSlide(4);
            else if (window.innerWidth >= 840) setItemsSlide(3);
            else setItemsSlide(2);

        }
        handleResize();

        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
        };
    }, []);

    const friendFundingData = memberFundingData?.data?.homeFriendFundingDtoList;

    return (
        <div className="mainFriendFunding">
            <div className="mainFriendFunding-text">
                친구를 위한 작은 선물! 지금 펀딩에 참여해보세요
            </div>

            <div className="friendFunding-contents">
                {friendFundingData && friendFundingData.length > 0 && (
                    <Carousel
                        showArrows={false}
                        showThumbs={false}
                        showStatus={false}
                        showIndicators={true}
                        emulateTouch={true}
                        arrows={false}
                        infinite={true}
                    >
                        {chunkArray(friendFundingData, itemsSlide).map((chunk, index) => (
                            <div key={index} className="mainFriendFundingitem">
                                {chunk.map((friendFunding, idx) => (
                                    <div className="mainFriendFundingContents">
                                        <Link key={idx}
                                              to={{
                                                  pathname: `/friend-Funding/Detail/${friendFunding.commonFriendFundingDto.fundingId}`,
                                                  state: { fundingId: friendFunding.commonFriendFundingDto.fundingId }
                                              }}
                                              className="mainFriendFundingLink"
                                              style={{ textDecoration: "none" }}
                                        >
                                            <div className="mainFriendFundingProfile-item">
                                                <ItemImg imageUrl={friendFunding.commonFriendFundingDto.friendFundingPageItemDtoList[0].itemImageUrl} className="mainFriendFunding-Itemimg"/>
                                                <img
                                                    src={toImageProxyUrl(friendFunding.commonFriendFundingDto.friendProfileImgUrl || defaultProfileImg)}
                                                    alt="프로필 이미지"
                                                    className="mainFriendFunding-profile-img"
                                                />
                                            </div>
                                            <div className="mainFriendFunding-textitem-GaugeBar">
                                                <div className="mainFriendFunding-textContents">
                                                    <div className="mainFriendFunding-NickName-Dday">
                                                        <div className="mainFriendFunding-NickName">{friendFunding.commonFriendFundingDto.nickName}</div>
                                                        <div className="mainFriendFunding-D-day">{friendFunding.commonFriendFundingDto.friendFundingDeadlineDate} </div>
                                                    </div>
                                                    <div>
                                                        <div className="mainFriendFunding-Tag">{friendFunding.commonFriendFundingDto.tag}</div>
                                                    </div>
                                                </div>
                                                <div className="mainFriendFunding-GaugeBarContent">
                                                    <GaugeBar value={friendFunding.commonFriendFundingDto.friendFundingPercent} className="mainFriendFunding-GaugeBar"/>
                                                </div>
                                            </div>
                                        </Link>
                                    </div>

                                ))}
                            </div>
                        ))}
                    </Carousel>
                )}
            </div>
        </div>
    );
};

export default MainFriendFunding;
