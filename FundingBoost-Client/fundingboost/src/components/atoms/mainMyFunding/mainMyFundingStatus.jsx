import React, { useState } from "react";

import ItemImg from "../itemImg/itemImg";
import GaugeBar from "../gauge-bar/gauge-bar";

function MainMyFundingStatus() {
    return (
        <div className="myFundingItem">
            <ItemImg className="myFundingItemimg"/>
            <GaugeBar className="myFundingGaugeBar"/>
        </div>
    )
}
export default MainMyFundingStatus