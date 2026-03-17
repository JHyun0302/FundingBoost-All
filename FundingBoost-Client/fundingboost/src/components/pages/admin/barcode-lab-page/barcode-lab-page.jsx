import React, { useCallback, useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { useNavigate, useSearchParams } from 'react-router-dom';
import HeaderBar from '../../../organisms/header/header';
import Footer from '../../../organisms/footer/footer';
import './barcode-lab-page.scss';

const TOKEN_MARKERS = [
    '/api/v1/admin/barcode-lab/tokens/',
    '/api/v1/pay/friends/barcode-token/'
];

const toDateObject = (value) => {
    if (!value) {
        return null;
    }
    if (Array.isArray(value)) {
        const [year, month, day, hour = 0, minute = 0, second = 0] = value;
        const date = new Date(year, Number(month) - 1, day, hour, minute, second);
        return Number.isNaN(date.getTime()) ? null : date;
    }
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? null : date;
};

const extractToken = (rawValue) => {
    const value = String(rawValue || '').trim();
    if (!value) {
        return '';
    }

    for (const marker of TOKEN_MARKERS) {
        if (value.includes(marker)) {
            const token = value.split(marker)[1]?.split('?')[0]?.trim();
            return token || '';
        }
    }

    return value;
};

const formatDateTime = (value) => {
    const date = toDateObject(value);
    if (!date) {
        return '-';
    }
    return date.toLocaleString('ko-KR', { hour12: false });
};

function BarcodeLabPage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const accessToken = localStorage.getItem('accessToken');

    const [accessState, setAccessState] = useState('loading');
    const [accessMessage, setAccessMessage] = useState('');
    const [scannerInput, setScannerInput] = useState('');
    const [verifyResult, setVerifyResult] = useState(null);
    const [verifyError, setVerifyError] = useState('');
    const [isVerifying, setIsVerifying] = useState(false);
    const [autoVerifyPending, setAutoVerifyPending] = useState(false);

    const parsedToken = useMemo(() => extractToken(scannerInput), [scannerInput]);

    const runVerify = useCallback(async (inputValue) => {
        const token = extractToken(inputValue);
        if (!token) {
            setVerifyError('스캔값(URL 또는 토큰)을 입력해주세요.');
            setVerifyResult(null);
            return;
        }
        if (!accessToken) {
            setVerifyError('로그인 토큰이 없습니다. 다시 로그인해주세요.');
            return;
        }

        setIsVerifying(true);
        setVerifyError('');
        try {
            const response = await axios.get(
                `${process.env.REACT_APP_FUNDINGBOOST}/admin/barcode-lab/tokens/${encodeURIComponent(token)}`,
                {
                    responseType: 'json',
                    headers: {
                        'Content-Type': 'application/json',
                        Authorization: `Bearer ${accessToken}`
                    }
                }
            );
            setVerifyResult(response?.data?.data || null);
        } catch (error) {
            const status = error?.response?.status;
            if (status === 403) {
                setVerifyError('관리자 권한이 없어서 검증할 수 없습니다.');
            } else if (status === 401) {
                setVerifyError('인증이 만료되었습니다. 다시 로그인해주세요.');
            } else {
                setVerifyError('검증에 실패했습니다. 토큰 만료/사용 여부를 확인해주세요.');
            }
            setVerifyResult(null);
        } finally {
            setIsVerifying(false);
        }
    }, [accessToken]);

    useEffect(() => {
        const tokenQuery = searchParams.get('token');
        if (!tokenQuery) {
            return;
        }
        setScannerInput(tokenQuery);
        if (searchParams.get('autoverify') === '1') {
            setAutoVerifyPending(true);
        }
    }, [searchParams]);

    useEffect(() => {
        if (accessState !== 'ready' || !autoVerifyPending || !parsedToken || isVerifying) {
            return;
        }
        setAutoVerifyPending(false);
        runVerify(parsedToken);
    }, [accessState, autoVerifyPending, parsedToken, isVerifying, runVerify]);

    useEffect(() => {
        if (!accessToken) {
            setAccessState('unauthenticated');
            setAccessMessage('로그인 후 관리자 권한으로 접근할 수 있습니다.');
            return;
        }

        const checkAccess = async () => {
            try {
                const accessResponse = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/admin/dashboard/access`, {
                    responseType: 'json',
                    headers: {
                        'Content-Type': 'application/json',
                        Authorization: `Bearer ${accessToken}`
                    }
                });
                const canAccess = accessResponse?.data?.data?.isSuccess === true;
                if (!canAccess) {
                    setAccessState('forbidden');
                    setAccessMessage('관리자 권한(ROLE_ADMIN) 계정만 접근할 수 있습니다.');
                    return;
                }
                setAccessState('ready');
                setAccessMessage('');
            } catch (error) {
                const status = error?.response?.status;
                if (status === 401) {
                    setAccessState('unauthenticated');
                    setAccessMessage('인증이 만료되었습니다. 다시 로그인해주세요.');
                    return;
                }
                if (status === 403) {
                    setAccessState('forbidden');
                    setAccessMessage('관리자 권한(ROLE_ADMIN) 계정만 접근할 수 있습니다.');
                    return;
                }
                setAccessState('error');
                setAccessMessage('관리자 페이지 접근 확인에 실패했습니다.');
            }
        };

        checkAccess();
    }, [accessToken]);

    const handleVerify = async () => {
        await runVerify(scannerInput);
    };

    return (
        <div className="barcode-lab-page">
            <HeaderBar />
            <main className="barcode-lab-main">
                <section className="barcode-lab-card">
                    <h1 className="barcode-lab-title">Admin Barcode Lab</h1>
                    <p className="barcode-lab-desc">
                        QRbot에서 읽은 Code128 결과(URL 또는 토큰)를 붙여넣어 바코드 상태를 검증합니다.
                    </p>
                    <button type="button" className="barcode-lab-back-button" onClick={() => navigate('/adm')}>
                        관리자 홈으로 이동
                    </button>

                    {accessState !== 'ready' && (
                        <div className={`barcode-lab-alert barcode-lab-alert-${accessState}`}>
                            <p>{accessMessage}</p>
                            {accessState !== 'forbidden' && (
                                <button type="button" onClick={() => navigate('/login')}>
                                    로그인 페이지로 이동
                                </button>
                            )}
                        </div>
                    )}

                    {accessState === 'ready' && (
                        <>
                            <div className="barcode-lab-input-wrap">
                                <label htmlFor="barcode-lab-input">스캔값 입력</label>
                                <textarea
                                    id="barcode-lab-input"
                                    value={scannerInput}
                                    onChange={(event) => setScannerInput(event.target.value)}
                                    placeholder="예: FBPAY-xxxx 또는 /api/v1/pay/friends/barcode-token/... URL"
                                    rows={3}
                                />
                                <p className="barcode-lab-token-preview">
                                    추출 토큰: <strong>{parsedToken || '-'}</strong>
                                </p>
                                <button type="button" onClick={handleVerify} disabled={isVerifying}>
                                    {isVerifying ? '검증 중...' : '바코드 토큰 검증'}
                                </button>
                            </div>

                            {verifyError && (
                                <p className="barcode-lab-error">{verifyError}</p>
                            )}

                            {verifyResult && (
                                <div className="barcode-lab-result">
                                    <h2>검증 결과</h2>
                                    <dl>
                                        <div>
                                            <dt>토큰</dt>
                                            <dd>{verifyResult.token || '-'}</dd>
                                        </div>
                                        <div>
                                            <dt>상태</dt>
                                            <dd>{verifyResult.status || '-'}</dd>
                                        </div>
                                        <div>
                                            <dt>펀딩 ID</dt>
                                            <dd>{verifyResult.fundingId ?? '-'}</dd>
                                        </div>
                                        <div>
                                            <dt>친구명</dt>
                                            <dd>{verifyResult.friendName || '-'}</dd>
                                        </div>
                                        <div>
                                            <dt>사용 포인트</dt>
                                            <dd>{Number(verifyResult.usingPoint || 0).toLocaleString()}P</dd>
                                        </div>
                                        <div>
                                            <dt>펀딩 금액</dt>
                                            <dd>{Number(verifyResult.fundingPrice || 0).toLocaleString()}원</dd>
                                        </div>
                                        <div>
                                            <dt>만료 시각</dt>
                                            <dd>{formatDateTime(verifyResult.expiresAt)}</dd>
                                        </div>
                                    </dl>
                                </div>
                            )}
                        </>
                    )}
                </section>
            </main>
            <Footer />
        </div>
    );
}

export default BarcodeLabPage;
