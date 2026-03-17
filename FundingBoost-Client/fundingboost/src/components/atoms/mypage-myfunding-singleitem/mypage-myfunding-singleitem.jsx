import React from "react";
import "./mypage-myfunding-singleitem.scss";
import Gauge from "../../atoms/gauge-bar/gauge-bar";
import MypageFinfundingbtn from "../../atoms/mypage-myfunding-fin-btn/mypage-myfunding-fin-btn";
import { toImageProxyUrl } from "../../../utils/imageProxyUrl";

const MyFundingSingleItem = ({ apiData, item, myPageFundingItemDtoList }) => {
    return (
        <div className="mypage-myfunding-box">
            <div className="mypage-myfunding-view">
                <img className="mypage-myfunding-image" alt="이미지" src={toImageProxyUrl(item.itemImageUrl)} />
                <div className="mypage-myfunding-text-wrapper-2">{item.itemPercent}%</div>
                <Gauge value={item.itemPercent} />
                <div className="mypage-myfunding-title-wrapper-3">{item.itemName}</div>
                <div className="mypage-myfunding-group">
                    <div className="mypage-myfunding-overlap-group">
                        <div className="mypage-myfunding-text-wrapper">옵션</div>
                    </div>
                    <div className="mypage-myfunding-div">{item.optionName}</div>
                </div>
                <div className="mypage-myfunding-text-wrapper-4">{item.itemPrice.toLocaleString()}원</div>

                {apiData && <MypageFinfundingbtn item={item} myPageFundingItemDtoList={myPageFundingItemDtoList} />}
            </div>
        </div>
    );
};

export default MyFundingSingleItem;
