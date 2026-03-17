import React, { useCallback, useEffect, useMemo, useState } from 'react';
import axios from 'axios';
import { Link, useNavigate } from 'react-router-dom';
import HeaderBar from '../../../organisms/header/header';
import Footer from '../../../organisms/footer/footer';
import './admin-dashboard-page.scss';
import { toImageProxyUrl } from "../../../../utils/imageProxyUrl";

const formatNumber = (value) => Number(value || 0).toLocaleString('ko-KR');
const ADMIN_MEMBER_PAGE_SIZE = 10;
const EDGE_TRAFFIC_STATUS_PATH = '/_edge/traffic-status';

const TRAFFIC_MODE_LABEL = {
    normal: '정상',
    offload: '정적 오프로딩',
    throttle: '이미지 프록시 제한'
};

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

const formatDateTime = (value) => {
    const date = toDateObject(value);
    if (!date) {
        return '-';
    }
    return date.toLocaleString('ko-KR', { hour12: false });
};

const formatDate = (value) => {
    const date = toDateObject(value);
    if (!date) {
        return '-';
    }
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, '0');
    const day = String(date.getDate()).padStart(2, '0');
    return `${year}.${month}.${day}`;
};

const getMemberIdFromToken = (token) => {
    if (!token) {
        return null;
    }
    try {
        const jwt = token.startsWith('Bearer ') ? token.slice(7) : token;
        const payloadPart = jwt.split('.')[1];
        if (!payloadPart) {
            return null;
        }
        const base64 = payloadPart
            .replace(/-/g, '+')
            .replace(/_/g, '/')
            .padEnd(Math.ceil(payloadPart.length / 4) * 4, '=');
        const payload = JSON.parse(window.atob(base64));
        const memberId = Number(payload?.sub);
        return Number.isFinite(memberId) ? memberId : null;
    } catch (error) {
        return null;
    }
};

const toDday = (deadline) => {
    const target = toDateObject(deadline);
    if (!target) {
        return '-';
    }
    const now = new Date();
    const diffMs = target.getTime() - now.getTime();
    if (diffMs <= 0) {
        return 'D-DAY';
    }
    const diffDays = Math.ceil(diffMs / (1000 * 60 * 60 * 24));
    return `D-${diffDays}`;
};

const toTrafficNumber = (value, fallback = 0) => {
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
};

const formatTrafficTb = (value) => toTrafficNumber(value).toFixed(4);

const formatTrafficPercent = (value) => `${toTrafficNumber(value).toFixed(2)}%`;

function AdminDashboardPage() {
    const navigate = useNavigate();
    const accessToken = localStorage.getItem('accessToken');
    const currentMemberId = useMemo(() => getMemberIdFromToken(accessToken), [accessToken]);

    const [accessState, setAccessState] = useState('loading');
    const [accessMessage, setAccessMessage] = useState('');
    const [dashboard, setDashboard] = useState(null);
    const [isLoadingDashboard, setIsLoadingDashboard] = useState(false);
    const [dashboardError, setDashboardError] = useState('');
    const [members, setMembers] = useState(null);
    const [membersLoading, setMembersLoading] = useState(false);
    const [membersError, setMembersError] = useState('');
    const [memberKeywordInput, setMemberKeywordInput] = useState('');
    const [memberKeyword, setMemberKeyword] = useState('');
    const [memberRoleFilter, setMemberRoleFilter] = useState('ALL');
    const [memberPage, setMemberPage] = useState(0);
    const [updatingMemberId, setUpdatingMemberId] = useState(null);
    const [trafficStatus, setTrafficStatus] = useState(null);
    const [trafficLoading, setTrafficLoading] = useState(false);
    const [trafficError, setTrafficError] = useState('');

    const authConfig = useMemo(
        () => ({
            responseType: 'json',
            headers: {
                'Content-Type': 'application/json',
                Authorization: `Bearer ${accessToken}`
            }
        }),
        [accessToken]
    );

    const loadDashboard = useCallback(async () => {
        setIsLoadingDashboard(true);
        setDashboardError('');
        try {
            const response = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/admin/dashboard`, authConfig);
            setDashboard(response?.data?.data || null);
        } catch (error) {
            setDashboardError('대시보드 데이터를 불러오지 못했습니다.');
        } finally {
            setIsLoadingDashboard(false);
        }
    }, [authConfig]);

    const loadMembers = useCallback(async () => {
        setMembersLoading(true);
        setMembersError('');
        try {
            const params = {
                page: memberPage,
                size: ADMIN_MEMBER_PAGE_SIZE
            };
            if (memberKeyword.trim()) {
                params.keyword = memberKeyword.trim();
            }
            if (memberRoleFilter !== 'ALL') {
                params.role = memberRoleFilter;
            }

            const response = await axios.get(
                `${process.env.REACT_APP_FUNDINGBOOST}/admin/members`,
                {
                    ...authConfig,
                    params
                }
            );
            setMembers(response?.data?.data || null);
        } catch (error) {
            setMembersError('회원 목록을 불러오지 못했습니다.');
        } finally {
            setMembersLoading(false);
        }
    }, [authConfig, memberKeyword, memberPage, memberRoleFilter]);

    useEffect(() => {
        if (!accessToken) {
            setAccessState('unauthenticated');
            setAccessMessage('로그인 후 관리자 페이지에 접근할 수 있습니다.');
            return;
        }

        const loadAdminDashboard = async () => {
            setAccessState('loading');
            setDashboardError('');

            try {
                const accessResponse = await axios.get(
                    `${process.env.REACT_APP_FUNDINGBOOST}/admin/dashboard/access`,
                    authConfig
                );
                const canAccess = accessResponse?.data?.data?.isSuccess === true;
                if (!canAccess) {
                    setAccessState('forbidden');
                    setAccessMessage('관리자 권한(ROLE_ADMIN) 계정만 접근할 수 있습니다.');
                    return;
                }
                setAccessState('ready');
                await loadDashboard();
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
                setDashboardError('대시보드 데이터를 불러오지 못했습니다.');
            }
        };

        loadAdminDashboard();
    }, [accessToken, authConfig, loadDashboard]);

    useEffect(() => {
        if (accessState !== 'ready') {
            return;
        }
        loadMembers();
    }, [accessState, loadMembers]);

    const handleMemberSearch = () => {
        setMemberPage(0);
        setMemberKeyword(memberKeywordInput);
    };

    const handleMemberRoleUpdate = async (memberId, nextRole) => {
        if (!memberId || !nextRole) {
            return;
        }
        setUpdatingMemberId(memberId);
        try {
            await axios.patch(
                `${process.env.REACT_APP_FUNDINGBOOST}/admin/members/${memberId}/role`,
                { role: nextRole },
                authConfig
            );
            await Promise.all([loadMembers(), loadDashboard()]);
        } catch (error) {
            alert('권한 변경에 실패했습니다. 마지막 관리자 계정은 일반 사용자로 변경할 수 없습니다.');
        } finally {
            setUpdatingMemberId(null);
        }
    };

    const moveToSection = useCallback((id) => {
        const target = document.getElementById(id);
        if (!target) {
            return;
        }
        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
    }, []);

    const moveToBarcodeLab = useCallback((token) => {
        if (!token) {
            return;
        }
        navigate(`/adm/barcode-lab?token=${encodeURIComponent(token)}&autoverify=1`);
    }, [navigate]);

    const loadTrafficStatus = useCallback(async () => {
        setTrafficLoading(true);
        setTrafficError('');
        try {
            const response = await axios.get(EDGE_TRAFFIC_STATUS_PATH, { responseType: 'json' });
            setTrafficStatus(response?.data || null);
        } catch (error) {
            setTrafficError('트래픽 상태를 불러오지 못했습니다.');
            setTrafficStatus(null);
        } finally {
            setTrafficLoading(false);
        }
    }, []);

    useEffect(() => {
        if (accessState !== 'ready') {
            return;
        }

        loadTrafficStatus();
        const interval = window.setInterval(loadTrafficStatus, 60_000);
        return () => window.clearInterval(interval);
    }, [accessState, loadTrafficStatus]);

    return (
        <div className="admin-dashboard-page">
            <HeaderBar />
            <main className="admin-dashboard-main">
                {accessState !== 'ready' && (
                    <section className={`admin-access-alert admin-access-alert-${accessState}`}>
                        <p>{accessMessage}</p>
                        {accessState !== 'forbidden' && (
                            <button type="button" onClick={() => navigate('/login')}>
                                로그인 페이지로 이동
                            </button>
                        )}
                    </section>
                )}

                {accessState === 'ready' && isLoadingDashboard && (
                    <section className="admin-loading">
                        <p>관리자 지표를 불러오는 중입니다...</p>
                    </section>
                )}

                {accessState === 'ready' && dashboardError && (
                    <section className="admin-error">
                        <p>{dashboardError}</p>
                    </section>
                )}

                {accessState === 'ready' && !isLoadingDashboard && dashboard && (
                    <>
                        <section className="admin-hero">
                            <div>
                                <span className="admin-hero-chip">FundingBoost Admin</span>
                                <h1>FundingBoost 관리자 센터</h1>
                                <h2 className="admin-hero-subtitle">실시간 운영 모니터링 · 권한/결제/상품 제어</h2>
                                <p className="admin-hero-hint">아래 운영 액션 바로가기를 통해 각 점검 영역으로 이동할 수 있습니다.</p>
                            </div>
                        </section>

                        <section className="admin-panel admin-operation-guide" id="admin-section-actions">
                            <header>
                                <h2>운영 액션 바로가기</h2>
                                <p>카드를 클릭하면 해당 관리 영역으로 이동합니다.</p>
                            </header>
                            <div className="admin-operation-grid">
                                <button type="button" className="admin-operation-card" onClick={() => moveToSection('admin-section-members')}>
                                    <h3>회원/권한 운영</h3>
                                    <p>관리자 승격·회수, 계정 역할 분리를 빠르게 처리합니다.</p>
                                    <span>권한 관리로 이동</span>
                                </button>
                                <button type="button" className="admin-operation-card" onClick={() => moveToSection('admin-section-tokens')}>
                                    <h3>결제 토큰 검증</h3>
                                    <p>미사용(PENDING) 토큰을 즉시 검증 화면으로 넘겨 확인합니다.</p>
                                    <span>토큰 모니터링으로 이동</span>
                                </button>
                                <button type="button" className="admin-operation-card" onClick={() => moveToSection('admin-section-top-items')}>
                                    <h3>상품/노출 최적화</h3>
                                    <p>카테고리/상품 지표를 보고 홈 랭킹 및 프로모션을 조정합니다.</p>
                                    <span>상품 지표로 이동</span>
                                </button>
                            </div>
                        </section>

                        <section className="admin-kpi-grid">
                            <article className="admin-kpi-card">
                                <h3>총 회원</h3>
                                <strong>{formatNumber(dashboard.kpi?.totalMembers)}명</strong>
                                <p>관리자 {formatNumber(dashboard.kpi?.adminMembers)}명 포함</p>
                            </article>
                            <article className="admin-kpi-card">
                                <h3>활성 펀딩</h3>
                                <strong>{formatNumber(dashboard.kpi?.activeFundings)}건</strong>
                                <p>누적 펀딩 {formatNumber(dashboard.kpi?.totalFundings)}건</p>
                            </article>
                            <article className="admin-kpi-card">
                                <h3>오늘 주문</h3>
                                <strong>{formatNumber(dashboard.kpi?.todayOrders)}건</strong>
                                <p>누적 주문 {formatNumber(dashboard.kpi?.totalOrders)}건</p>
                            </article>
                            <article className="admin-kpi-card">
                                <h3>거래액(GMV)</h3>
                                <strong>{formatNumber(dashboard.kpi?.totalRevenue)}원</strong>
                                <p>오늘 {formatNumber(dashboard.kpi?.todayRevenue)}원</p>
                            </article>
                            <article className={`admin-kpi-card admin-kpi-card-traffic ${trafficStatus?.mode ? `mode-${trafficStatus.mode}` : ''}`}>
                                <h3>월 누적 트래픽 (Edge)</h3>
                                <strong>
                                    {trafficLoading
                                        ? '조회 중...'
                                        : `${formatTrafficTb(trafficStatus?.monthlyTb)} / ${toTrafficNumber(trafficStatus?.thresholdTb, 10)}TB`}
                                </strong>
                                <p>
                                    {trafficError || `임계치 사용률 ${formatTrafficPercent(trafficStatus?.thresholdRatio)}`}
                                </p>
                                {!trafficError && trafficStatus && (
                                    <small>
                                        모드: {TRAFFIC_MODE_LABEL[trafficStatus.mode] || trafficStatus.mode} · 기준월: {trafficStatus.month || '-'}
                                    </small>
                                )}
                            </article>
                        </section>

                        <section className="admin-panel admin-two-column">
                            <div className="admin-panel-block" id="admin-section-expiring">
                                <header>
                                    <h2>마감 임박 펀딩 (72시간)</h2>
                                    <p>전체 사용자 기준</p>
                                </header>
                                <div className="admin-list">
                                    {(dashboard.expiringFundings || []).map((funding) => (
                                        <div key={funding.fundingId} className="admin-list-item">
                                            <div className="admin-list-item-left">
                                                <strong>{funding.ownerName}</strong>
                                                <span>{funding.tag || '-'}</span>
                                                <small>{formatNumber(funding.collectPrice)} / {formatNumber(funding.totalPrice)}원</small>
                                            </div>
                                            <div className="admin-list-item-right">
                                                <b>{toDday(funding.deadline)}</b>
                                                <span>{funding.progressPercent}%</span>
                                            </div>
                                        </div>
                                    ))}
                                    {(!dashboard.expiringFundings || dashboard.expiringFundings.length === 0) && (
                                        <p className="admin-empty">72시간 이내 마감되는 활성 펀딩이 없습니다.</p>
                                    )}
                                </div>
                            </div>

                            <div className="admin-panel-block" id="admin-section-tokens">
                                <header>
                                    <h2>바코드 토큰 모니터링</h2>
                                    <Link to="/adm/barcode-lab">검증 바로가기</Link>
                                </header>
                                <div className="admin-token-summary">
                                    <div>
                                        <span>PENDING</span>
                                        <strong>{formatNumber(dashboard.barcodeTokenSummary?.pendingCount)}</strong>
                                    </div>
                                    <div>
                                        <span>USED</span>
                                        <strong>{formatNumber(dashboard.barcodeTokenSummary?.usedCount)}</strong>
                                    </div>
                                    <div>
                                        <span>EXPIRED</span>
                                        <strong>{formatNumber(dashboard.barcodeTokenSummary?.expiredCount)}</strong>
                                    </div>
                                </div>
                                <div className="admin-token-list">
                                    {(dashboard.recentBarcodeTokens || []).map((token) => (
                                        <button
                                            key={`${token.token}-${token.issuedAt}`}
                                            type="button"
                                            className={`admin-token-item ${token.status === 'PENDING' ? 'is-clickable' : ''}`}
                                            onClick={() => {
                                                if (token.status === 'PENDING') {
                                                    moveToBarcodeLab(token.token);
                                                }
                                            }}
                                            disabled={token.status !== 'PENDING'}
                                        >
                                            <div>
                                                <strong>{token.status}</strong>
                                                <span>{token.issuedBy} → {token.fundingOwner}</span>
                                            </div>
                                            <div className="admin-token-item-right">
                                                <small>{formatDateTime(token.issuedAt)}</small>
                                                {token.status === 'PENDING' && <span>클릭 시 즉시 검증</span>}
                                            </div>
                                        </button>
                                    ))}
                                    {(!dashboard.recentBarcodeTokens || dashboard.recentBarcodeTokens.length === 0) && (
                                        <p className="admin-empty">발급된 바코드 토큰이 없습니다.</p>
                                    )}
                                </div>
                            </div>
                        </section>

                        <section className="admin-panel admin-table-panel" id="admin-section-top-items">
                            <header>
                                <h2>인기 상품 TOP 10</h2>
                                <p>점수 = 펀딩 * 3 + 구매수량 * 2 + 위시 * 1</p>
                            </header>
                            <div className="admin-table-wrap">
                                <table>
                                    <thead>
                                    <tr>
                                        <th>상품</th>
                                        <th>카테고리</th>
                                        <th>펀딩</th>
                                        <th>구매수량</th>
                                        <th>위시</th>
                                        <th>점수</th>
                                    </tr>
                                    </thead>
                                    <tbody>
                                    {(dashboard.topItems || []).map((item) => (
                                        <tr key={item.itemId}>
                                            <td>
                                                <button
                                                    type="button"
                                                    className="admin-item-cell admin-item-link"
                                                    onClick={() => navigate(`/shopping/detail/${item.itemId}`)}
                                                >
                                                    <img src={toImageProxyUrl(item.itemImageUrl)} alt={item.itemName} />
                                                    <div>
                                                        <strong>{item.itemName}</strong>
                                                        <span>{item.brandName}</span>
                                                    </div>
                                                </button>
                                            </td>
                                            <td>{item.category}</td>
                                            <td>{formatNumber(item.fundingCount)}</td>
                                            <td>{formatNumber(item.orderQuantity)}</td>
                                            <td>{formatNumber(item.wishlistCount)}</td>
                                            <td>{formatNumber(item.score)}</td>
                                        </tr>
                                    ))}
                                    {(!dashboard.topItems || dashboard.topItems.length === 0) && (
                                        <tr>
                                            <td className="admin-empty" colSpan={6}>집계할 상품 활동 데이터가 없습니다.</td>
                                        </tr>
                                    )}
                                    </tbody>
                                </table>
                            </div>
                        </section>

                        <section className="admin-panel admin-two-column">
                            <div className="admin-panel-block">
                                <header>
                                    <h2>카테고리 퍼포먼스</h2>
                                </header>
                                <div className="admin-table-wrap">
                                    <table>
                                        <thead>
                                        <tr>
                                            <th>카테고리</th>
                                            <th>펀딩</th>
                                            <th>구매</th>
                                            <th>위시</th>
                                            <th>점수</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {(dashboard.categoryMetrics || []).map((metric) => (
                                            <tr key={metric.category}>
                                                <td>{metric.category}</td>
                                                <td>{formatNumber(metric.fundingCount)}</td>
                                                <td>{formatNumber(metric.orderQuantity)}</td>
                                                <td>{formatNumber(metric.wishlistCount)}</td>
                                                <td>{formatNumber(metric.score)}</td>
                                            </tr>
                                        ))}
                                        {(!dashboard.categoryMetrics || dashboard.categoryMetrics.length === 0) && (
                                            <tr>
                                                <td className="admin-empty" colSpan={5}>카테고리 통계 데이터가 없습니다.</td>
                                            </tr>
                                        )}
                                        </tbody>
                                    </table>
                                </div>
                            </div>

                            <div className="admin-panel-block">
                                <header>
                                    <h2>최근 가입 회원</h2>
                                </header>
                                <div className="admin-list">
                                    {(dashboard.recentMembers || []).map((member) => (
                                        <div key={member.memberId} className="admin-list-item">
                                            <div className="admin-list-item-left">
                                                <strong>{member.nickName}</strong>
                                                <span>{member.email}</span>
                                                <small>{member.role} / {member.gender}</small>
                                            </div>
                                            <div className="admin-list-item-right">
                                                <small>{formatDateTime(member.joinedAt)}</small>
                                            </div>
                                        </div>
                                    ))}
                                    {(!dashboard.recentMembers || dashboard.recentMembers.length === 0) && (
                                        <p className="admin-empty">최근 가입 데이터가 없습니다.</p>
                                    )}
                                </div>
                            </div>
                        </section>

                        <section className="admin-panel admin-member-panel" id="admin-section-members">
                            <header>
                                <h2>회원 권한 관리</h2>
                                <p>관리자/일반 사용자 권한을 운영자가 직접 변경할 수 있습니다.</p>
                            </header>
                            <div className="admin-member-toolbar">
                                <input
                                    type="text"
                                    placeholder="닉네임/이메일 검색"
                                    value={memberKeywordInput}
                                    onChange={(event) => setMemberKeywordInput(event.target.value)}
                                    onKeyDown={(event) => {
                                        if (event.key === 'Enter') {
                                            handleMemberSearch();
                                        }
                                    }}
                                />
                                <select
                                    value={memberRoleFilter}
                                    onChange={(event) => {
                                        setMemberRoleFilter(event.target.value);
                                        setMemberPage(0);
                                    }}
                                >
                                    <option value="ALL">전체 권한</option>
                                    <option value="ROLE_ADMIN">관리자</option>
                                    <option value="ROLE_USER">사용자</option>
                                </select>
                                <button type="button" onClick={handleMemberSearch}>
                                    검색
                                </button>
                            </div>

                            {membersLoading && <p className="admin-empty">회원 목록을 불러오는 중입니다...</p>}
                            {membersError && <p className="admin-empty">{membersError}</p>}

                            {!membersLoading && !membersError && (
                                <div className="admin-table-wrap">
                                    <table>
                                        <thead>
                                        <tr>
                                            <th>ID</th>
                                            <th>닉네임</th>
                                            <th>이메일</th>
                                            <th>권한</th>
                                            <th>성별</th>
                                            <th>포인트</th>
                                            <th>가입일</th>
                                            <th>관리</th>
                                        </tr>
                                        </thead>
                                        <tbody>
                                        {(members?.content || []).map((member) => {
                                            const nextRole = member.role === 'ROLE_ADMIN' ? 'ROLE_USER' : 'ROLE_ADMIN';
                                            const isSelf = currentMemberId !== null
                                                && Number(member.memberId) === Number(currentMemberId);
                                            return (
                                                <tr key={member.memberId}>
                                                    <td>{member.memberId}</td>
                                                    <td>{member.nickName}</td>
                                                    <td>{member.email}</td>
                                                    <td>{member.role}</td>
                                                    <td>{member.gender}</td>
                                                    <td>{formatNumber(member.point)}P</td>
                                                    <td>{formatDate(member.joinedAt)}</td>
                                                    <td>
                                                        <button
                                                            type="button"
                                                            className={isSelf
                                                                ? 'role-self'
                                                                : (member.role === 'ROLE_ADMIN' ? 'role-demote' : 'role-promote')}
                                                            disabled={updatingMemberId === member.memberId || isSelf}
                                                            onClick={() => handleMemberRoleUpdate(member.memberId, nextRole)}
                                                        >
                                                            {isSelf
                                                                ? '내 계정'
                                                                : updatingMemberId === member.memberId
                                                                ? '처리 중...'
                                                                : member.role === 'ROLE_ADMIN'
                                                                    ? '사용자로 변경'
                                                                    : '관리자로 승격'}
                                                        </button>
                                                    </td>
                                                </tr>
                                            );
                                        })}
                                        {(!members?.content || members.content.length === 0) && (
                                            <tr>
                                                <td className="admin-empty" colSpan={8}>조건에 맞는 회원이 없습니다.</td>
                                            </tr>
                                        )}
                                        </tbody>
                                    </table>
                                </div>
                            )}

                            <div className="admin-member-pagination">
                                <button
                                    type="button"
                                    disabled={!members || memberPage <= 0 || membersLoading}
                                    onClick={() => setMemberPage((prev) => Math.max(prev - 1, 0))}
                                >
                                    이전
                                </button>
                                <span>
                                    {(members?.page || 0) + 1} / {Math.max(members?.totalPages || 1, 1)}
                                </span>
                                <button
                                    type="button"
                                    disabled={!members || !members.hasNext || membersLoading}
                                    onClick={() => setMemberPage((prev) => prev + 1)}
                                >
                                    다음
                                </button>
                            </div>
                        </section>
                    </>
                )}
            </main>
            <Footer />
        </div>
    );
}

export default AdminDashboardPage;
