import ProfileImg from "../../../atoms/ProfileImg/ProfileImg";
import CheckFundingButton from "../../../atoms/button/mainMyfuudingBtn/checkFunding-btn";
import React, { useState, useEffect } from "react";
import GaugeBar from "../../../atoms/gauge-bar/gauge-bar";
import { Carousel } from 'react-responsive-carousel';
import 'react-responsive-carousel/lib/styles/carousel.min.css';
import './memberYesFunding.scss';
import { toImageProxyUrl } from "../../../../utils/imageProxyUrl";

function MemberYesFunding({ memberFundingData }) {
    const [isMobile, setIsMobile] = useState(window.innerWidth <= 1085);

    //반응형 추가
    useEffect(() => {
        const handleResize = () => {
            setIsMobile(window.innerWidth <= 1085);
        };
        handleResize();
        window.addEventListener('resize', handleResize);

        return () => {
            window.removeEventListener('resize', handleResize);
        };
    }, []);

    console.log("상품이미지:", memberFundingData?.data?.homeMyFundingItemDtoList?.itemImageUrl);

    //캐러셀 분할
    function chunkArray(arr, chunkSize) {
        const chunkedArray = [];
        for (let i = 0; i < arr.length; i += chunkSize) {
            chunkedArray.push(arr.slice(i, i + chunkSize));
        }
        return chunkedArray;
    }

    return (
        <div className="memberYesFunding">
            <div className="memberYesFundingstatus">
                <div className="memberYesFunding-item">
                    <ProfileImg className="memberYesFunding-Profile" memberFundingData={memberFundingData.data} />
                    <div className="memberYesFunding-text">
                        <div className="memberYesFunding-text">
                            <div className="myfundingNickName">{memberFundingData?.data?.homeMemberInfoDto?.nickName}님</div>
                            펀딩 현황
                        </div>
                        {/*deadline에 따른 렌더링, "종료"일 경우 추가 */}
                        <div className={`memberFundingD-day ${memberFundingData?.data?.homeMyFundingStatusDto?.deadline === "종료"? "memberFundingD-dayEnd" :""}`}>
                            {memberFundingData?.data?.homeMyFundingStatusDto?.deadline === "종료"? "펀딩이 종료되었습니다!" :memberFundingData?.data?.homeMyFundingStatusDto?.deadline }
                        </div>
                        <div className="memberFundingTag">{memberFundingData?.data?.homeMyFundingStatusDto?.tag}</div>
                    </div>
                    <div className="memberFunding-RightItem">
                        <div className="memberFundingProgress">{memberFundingData?.data?.homeMyFundingStatusDto?.totalPercent}%</div>
                        <CheckFundingButton/>
                    </div>
                </div>

                {/* 반응형 Carousel 추가 */}
                {!isMobile ? (
                    <div className="myFundingItemsContainer mobile-carousel">
                        <div className="myFundingItems">
                            {memberFundingData?.data?.homeMyFundingStatusDto?.homeMyFundingItemDtoList?.map((product, index) => (
                                <div className="myFundingItem-a" key={index}>
                                    <div className="myFundingItem">
                                        <img src={toImageProxyUrl(product.itemImageUrl)} // 각 상품의 percent가 100인 경우 관리
                                             className={`myFundingItemimg ${product.itemPercent === 100 ? 'gaugeFull' : 'myFundingItemImg'}`} />
                                        <GaugeBar value={product.itemPercent} className="myFundingGaugeBar"/>
                                    </div>
                                </div>
                            ))}
                        </div>
                    </div>
                ) : (
                    <div className="myFundingItemsContainer mobile-carousel">
                        <div className="myFundingItems">
                            {/*캐러셀 커스텀*/}
                            <Carousel
                                showArrows={true}
                                showThumbs={false}
                                showStatus={false}
                                showIndicators={false}
                                emulateTouch={true}
                                arrows={false}
                                autoPlay={false}
                                // interval={2000}
                                infinite={true}
                            >
                                {chunkArray(memberFundingData?.data?.homeMyFundingStatusDto?.homeMyFundingItemDtoList, 3).map((chunk, index) => (
                                    <div className="myFundingItem-a" key={index}>
                                        {chunk.map((product, index) => (
                                            <div className="myFundingItem" key={index}>
                                                <img src={toImageProxyUrl(product.itemImageUrl)}
                                                     className={`myFundingItemimg ${product.itemPercent === 100 ? 'gaugeFull' : 'myFundingItemImg'}`}/>
                                                <GaugeBar value={product.itemPercent} className="myFundingGaugeBar"/>
                                            </div>
                                        ))}
                                    </div>
                                ))}
                            </Carousel>
                        </div>
                    </div>
                )}
            </div>
        </div>
    );
}

export default MemberYesFunding;
