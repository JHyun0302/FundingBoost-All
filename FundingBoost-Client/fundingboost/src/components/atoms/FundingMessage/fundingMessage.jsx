import React, { useState, useEffect } from 'react';
import './fundingMessage.scss';

export default function FundingMessage({ initialMessage, onMessageChange }) {
    const [inputCount, setInputCount] = useState(0);
    const [inputText, setInputText] = useState("");

    useEffect(() => {
        const normalizedMessage = typeof initialMessage === "string" ? initialMessage : "";
        setInputText(normalizedMessage);
        setInputCount(normalizedMessage.length);
    }, [initialMessage]);

    const onInputHandler = (e) => {
        const text = e.target.value;
        setInputText(text);
        setInputCount(text.length);
        onMessageChange(text);
    };

    return (
        <div className="fundingMessageBox">
            <div className="fundingMessageView">
                <div className="fundingMessageTitle">
                    <span className="fundingMessageTitleText1">📜 펀딩 메세지 </span>
                    <span className="fundingMessageTitleText2">(선택)</span>
                </div>
                <input
                    type="text"
                    className="fundingMessage"
                    maxLength={19}
                    onChange={onInputHandler}
                    value={inputText}
                    placeholder="펀딩을 소개해보세요"
                />
                <p className="textCount">
                    <span>{inputCount}</span>
                    <span>/20 자</span>
                </p>
            </div>
        </div>
    );
}
