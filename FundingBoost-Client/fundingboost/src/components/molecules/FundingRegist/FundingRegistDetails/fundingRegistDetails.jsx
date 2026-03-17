import React, { useState, useEffect } from 'react';
import FundingMessage from "../../../atoms/FundingMessage/fundingMessage";
import FundingTagBtn from "../../../atoms/button/FundingTagBtn/fundingTagBtn";
import Calender from "../../../atoms/Calendar/calender";
import './fundingRegistDetails.scss';

function FundingRegistDetails({ onTagSelect, onMessageChange, onDateChange, messageText, endDate }) {
    const [selectedEndDate, setSelectedEndDate] = useState(endDate || new Date());
    const [selectedMessage, setSelectedMessage] = useState(messageText || "");

    useEffect(() => {
        if (!endDate) {
            const defaultEndDate = new Date();
            defaultEndDate.setDate(defaultEndDate.getDate() + 13);
            setSelectedEndDate(defaultEndDate);
            onDateChange(defaultEndDate);
        }
    }, [onDateChange, endDate]);

    const handleTagSelect = (tagText) => {
        onTagSelect(tagText);

        // 태그 선택 시 해당 태그의 메시지로 설정
        const tagMessageMap = {
            생일: "생일이에요🎉 축하해주세요",
            졸업: "졸업했어요🧑‍🎓 축하해주세요",
            기타: "펀딩 해주세요🎁"
        };
        const tagMessage = tagMessageMap[tagText] || "";
        setSelectedMessage(tagMessage);
        onMessageChange(tagMessage);
    };

    const handleMessageChange = (messageText) => {
        setSelectedMessage(messageText);
        onMessageChange(messageText);
    };

    const handleEndDate = (endDate) => {
        setSelectedEndDate(endDate);
        onDateChange(endDate);
    };

    return (
        <div className="fundingRegistDetails">
            <div className="fundingRegistDetailsContent">
                <div>
                    <Calender onDateChange={handleEndDate} selectedDate={selectedEndDate} />
                    <FundingTagBtn onTagSelect={handleTagSelect} />
                    <FundingMessage initialMessage={selectedMessage} onMessageChange={handleMessageChange} />
                </div>
            </div>
        </div>
    );
}

export default FundingRegistDetails;
