import React, { useEffect, useMemo, useRef, useState } from 'react';
import axios from "axios";
import JsBarcode from "jsbarcode";
import FriendFundingPaymentBtn from "../../../atoms/button/friendfundingPaymentBtn/friendfundingPaymentBtn";
import './friendFundingPay-barcode.scss';

const FriendFundingPayBarcode = ({finalPrice, fundingAmount, usePoints, fundingId}) => {
    const barcodeSvgRef = useRef(null);
    const [tokenIssue, setTokenIssue] = useState(null);
    const [issueError, setIssueError] = useState('');
    const [scannerInput, setScannerInput] = useState('');
    const [verifyResult, setVerifyResult] = useState(null);
    const [verifyError, setVerifyError] = useState('');
    const [isVerifying, setIsVerifying] = useState(false);

    const normalizedFinalPrice = Number(String(finalPrice || 0).replaceAll(',', '')) || 0;
    const normalizedFundingAmount = Math.max(Number(fundingAmount || 0), 0);
    const normalizedUsePoints = Math.max(Number(usePoints || 0), 0);

    const barcodeTokenValue = tokenIssue?.barcodeValue || '';
    const verifyUrl = tokenIssue?.verifyUrl || '';
    const debugByQuery = useMemo(() => {
        const query = new URLSearchParams(window.location.search);
        return query.get('barcodeDebug') === '1';
    }, []);
    const isBarcodeDebugMode = process.env.REACT_APP_BARCODE_DEBUG === 'true' || debugByQuery;

    const extractToken = (rawValue) => {
        const value = String(rawValue || '').trim();
        if (!value) {
            return '';
        }
        const marker = '/api/v1/pay/friends/barcode-token/';
        if (value.includes(marker)) {
            const token = value.split(marker)[1]?.split('?')[0]?.trim();
            return token || '';
        }
        return value;
    };

    const tokenExpiryLabel = useMemo(() => {
        if (!tokenIssue?.expiresAt) {
            return '';
        }
        const expiryDate = new Date(tokenIssue.expiresAt);
        if (Number.isNaN(expiryDate.getTime())) {
            return '';
        }
        return expiryDate.toLocaleTimeString('ko-KR', {hour12: false});
    }, [tokenIssue]);

    useEffect(() => {
        if (!fundingId || normalizedFundingAmount <= 0) {
            setTokenIssue(null);
            setIssueError('');
            return;
        }

        const accessToken = localStorage.getItem('accessToken');
        if (!accessToken) {
            setIssueError('로그인 후 바코드 토큰을 발급할 수 있습니다.');
            setTokenIssue(null);
            return;
        }

        const timer = setTimeout(async () => {
            try {
                const response = await axios.post(
                    `${process.env.REACT_APP_FUNDINGBOOST}/pay/friends/${fundingId}/barcode-token`,
                    {
                        usingPoint: normalizedUsePoints,
                        fundingPrice: normalizedFundingAmount
                    },
                    {
                        responseType: 'json',
                        headers: {
                            "Content-Type": "application/json",
                            "Authorization": `Bearer ${accessToken}`
                        }
                    }
                );
                setTokenIssue(response.data?.data || null);
                setIssueError('');
            } catch (error) {
                console.error('바코드 토큰 발급 실패:', error);
                setTokenIssue(null);
                setIssueError('바코드 토큰 발급에 실패했습니다. 금액/포인트를 확인해주세요.');
            }
        }, 250);

        return () => clearTimeout(timer);
    }, [fundingId, normalizedFundingAmount, normalizedUsePoints]);

    useEffect(() => {
        if (!barcodeSvgRef.current || !barcodeTokenValue) {
            return;
        }

        try {
            JsBarcode(barcodeSvgRef.current, barcodeTokenValue, {
                format: 'CODE128',
                displayValue: false,
                lineColor: '#000000',
                width: 1.6,
                height: 116,
                margin: 8,
                background: '#ffffff'
            });
        } catch (error) {
            console.error('바코드 렌더링 실패:', error);
        }
    }, [barcodeTokenValue]);

    const handleVerifyToken = async () => {
        const token = extractToken(scannerInput);
        if (!token) {
            setVerifyError('스캔한 URL 또는 토큰 값을 입력하세요.');
            setVerifyResult(null);
            return;
        }
        setIsVerifying(true);
        setVerifyError('');
        try {
            const response = await axios.get(
                `${process.env.REACT_APP_FUNDINGBOOST}/pay/friends/barcode-token/${encodeURIComponent(token)}`,
                {
                    responseType: 'json',
                    headers: {
                        "Content-Type": "application/json"
                    }
                }
            );
            setVerifyResult(response.data?.data || null);
        } catch (error) {
            console.error('토큰 검증 실패:', error);
            setVerifyResult(null);
            setVerifyError('토큰 검증에 실패했습니다. 만료되었거나 유효하지 않습니다.');
        } finally {
            setIsVerifying(false);
        }
    };

    const handleCopyVerifyUrl = async () => {
        if (!verifyUrl) {
            return;
        }
        try {
            await navigator.clipboard.writeText(verifyUrl);
            alert('검증 URL을 복사했습니다.');
        } catch (error) {
            console.error('URL 복사 실패:', error);
        }
    };

    return (
        <div className="friend-funding-bar-code">
            <div className="friend-funding-bar-code-image">
                {barcodeTokenValue ? (
                    <svg ref={barcodeSvgRef} role="img" aria-label="결제 바코드" />
                ) : (
                    <div className="friend-funding-bar-code-placeholder">
                        바코드 토큰 준비 중...
                    </div>
                )}
            </div>
            <div className="friend-funding-bar-code-meta">
                {!tokenIssue?.token && (
                    <p className="friend-funding-bar-code-expire">{issueError || '결제 바코드 생성 중입니다.'}</p>
                )}

                {isBarcodeDebugMode && tokenIssue?.token && (
                    <>
                        <p className="friend-funding-bar-code-token-label">[디버그] 토큰</p>
                        <p className="friend-funding-bar-code-token-value">{tokenIssue.token}</p>
                        {verifyUrl && (
                            <>
                                <p className="friend-funding-bar-code-token-label">[디버그] 검증 URL</p>
                                <p className="friend-funding-bar-code-token-value">{verifyUrl}</p>
                                <button
                                    type="button"
                                    className="friend-funding-bar-code-copy-btn"
                                    onClick={handleCopyVerifyUrl}
                                >
                                    검증 URL 복사
                                </button>
                            </>
                        )}
                        {tokenExpiryLabel && (
                            <p className="friend-funding-bar-code-expire">[디버그] 만료 시각: {tokenExpiryLabel}</p>
                        )}
                    </>
                )}
            </div>
            <div className="friend-funding-bar-code-text">
                <div className="friend-funding-bar-code-price">₩&nbsp;&nbsp;&nbsp;&nbsp;{normalizedFinalPrice.toLocaleString()}원</div>
                <FriendFundingPaymentBtn
                    className="friend-funding-bar-code-button"
                    fundingId={fundingId}
                    barcodeToken={tokenIssue?.token || ''}
                    disabled={!tokenIssue?.token}
                />
            </div>
            {isBarcodeDebugMode && (
                <div className="friend-funding-barcode-verify">
                    <p className="friend-funding-barcode-verify-title">QRbot 스캔 테스트 (디버그)</p>
                    <p className="friend-funding-barcode-verify-help">
                        QRbot으로 바코드를 스캔하면 토큰 텍스트가 반환됩니다. URL/토큰을 아래에 붙여넣어 검증할 수 있습니다.
                    </p>
                    <div className="friend-funding-barcode-verify-row">
                        <input
                            className="friend-funding-barcode-verify-input"
                            value={scannerInput}
                            onChange={(event) => setScannerInput(event.target.value)}
                            placeholder="QRbot에서 읽은 URL 또는 토큰을 붙여넣으세요."
                        />
                        <button
                            type="button"
                            className="friend-funding-barcode-verify-btn"
                            onClick={handleVerifyToken}
                            disabled={isVerifying}
                        >
                            {isVerifying ? '검증 중...' : '토큰 검증'}
                        </button>
                    </div>
                    {verifyError && <p className="friend-funding-barcode-verify-error">{verifyError}</p>}
                    {verifyResult && (
                        <p className="friend-funding-barcode-verify-success">
                            상태: {verifyResult.status} / 금액: {Number(verifyResult.fundingPrice || 0).toLocaleString()}원
                        </p>
                    )}
                </div>
            )}
        </div>
    );
};

export default FriendFundingPayBarcode;
