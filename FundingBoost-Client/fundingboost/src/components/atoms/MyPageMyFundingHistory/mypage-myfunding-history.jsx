import React from 'react';
import "./mypage-myfunding-history.scss";
import MypageMyfundingGauge from "../mypage-myfunding-gauge/mypage-myfunding-gauge";
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

export default function MyPageMyFundingHistory({ data, onOpenDetail }) {
    console.log(data);

    return (
        <div className="myPageFundingRecordItem-container">
            {data && (
                <button type="button" className="myPageFundingRecordItem" onClick={() => onOpenDetail(data.fundingId)}>
                    <img className="image" alt="Image" src={toImageProxyUrl(data.itemImageUrl)} />
                    <div className="myPageFundingRecordText">
                        <div className='showFunding'>
                            <div className="fundingTag">{data.tag}</div>
                            <div className='historyCardHint'>클릭하여 상세 보기</div>
                        </div>
                        <div className="historyItemName">{data.itemName}</div>
                        <p className="fundingDateTime">
                            펀딩 시작일 : {data.createdDate}
                            <br />
                            펀딩 종료일 : {data.deadLine}
                        </p>
                        <div className='showFundingGauge'>
                            <div className="participatePeople">{data.contributorCount}명 참여</div>
                            {/*<div className="fundingGaugePercent">{data.status ? "100%" : 종료}</div>*/}
                        </div>
                        <MypageMyfundingGauge totalPercent={data.fundingPercent} />
                    </div>
                </button>
            )}
        </div>
    );
};
