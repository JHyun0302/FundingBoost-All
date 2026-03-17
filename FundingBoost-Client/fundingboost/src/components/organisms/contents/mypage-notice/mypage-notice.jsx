import React, { useEffect, useMemo, useRef, useState } from 'react';
import axios from 'axios';
import './mypage-notice.scss';
import MypageProfile from '../../../molecules/MypageProfile/mypageprofile';
import MyPageIndex from '../../../molecules/MypageIndex/mypageindex';
import NonMemberModal from '../../../atoms/nonMemberModal/nonMemberModal';

const INITIAL_NOTICE_FORM = {
    category: '안내',
    title: '',
    body: ''
};
const NOTICE_PAGE_SIZE = 5;

const MypageNoticePane = () => {
    const [profileData, setProfileData] = useState(null);
    const [modalShowState, setModalShowState] = useState(false);
    const [notices, setNotices] = useState([]);
    const [loading, setLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState('');
    const [isAdmin, setIsAdmin] = useState(false);
    const [noticeForm, setNoticeForm] = useState(INITIAL_NOTICE_FORM);
    const [editingNoticeId, setEditingNoticeId] = useState(null);
    const [saving, setSaving] = useState(false);
    const [noticePage, setNoticePage] = useState(0);
    const noticeEditorRef = useRef(null);
    const noticeTitleInputRef = useRef(null);

    const handleButtonClick = (index) => {
        console.log(`Selected index: ${index}`);
    };

    const accessToken = localStorage.getItem('accessToken');

    const authConfig = {
        headers: {
            Authorization: `Bearer ${accessToken}`
        },
        responseType: 'json'
    };

    const formatDate = (value) => {
        if (!value) {
            return '-';
        }
        const date = new Date(value);
        if (Number.isNaN(date.getTime())) {
            return '-';
        }
        const year = date.getFullYear();
        const month = String(date.getMonth() + 1).padStart(2, '0');
        const day = String(date.getDate()).padStart(2, '0');
        return `${year}.${month}.${day}`;
    };

    const loadNotices = async () => {
        const response = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/notices`, authConfig);
        const nextNotices = response?.data?.data?.notices || [];
        setNotices(nextNotices);
    };

    const noticeTotalPages = useMemo(
        () => Math.max(Math.ceil(notices.length / NOTICE_PAGE_SIZE), 1),
        [notices.length]
    );

    const pagedNotices = useMemo(() => {
        const start = noticePage * NOTICE_PAGE_SIZE;
        return notices.slice(start, start + NOTICE_PAGE_SIZE);
    }, [notices, noticePage]);

    useEffect(() => {
        const fetchData = async () => {
            try {
                if (!accessToken) {
                    setModalShowState(true);
                    setLoading(false);
                    return;
                }

                const [profileResponse, noticeResponse, accessResponse] = await Promise.all([
                    axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/funding/my-funding-status`, authConfig),
                    axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/notices`, authConfig),
                    axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/admin/dashboard/access`, authConfig).catch(() => null)
                ]);

                setProfileData(profileResponse?.data?.data || null);
                setNotices(noticeResponse?.data?.data?.notices || []);
                setIsAdmin(accessResponse?.data?.data?.isSuccess === true);
                setErrorMessage('');
            } catch (error) {
                console.error('공지사항 정보를 불러오는 중 오류가 발생했습니다.', error);
                setErrorMessage('공지사항을 불러오지 못했습니다.');
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [accessToken]);

    useEffect(() => {
        setNoticePage((prev) => {
            const maxPage = Math.max(noticeTotalPages - 1, 0);
            return prev > maxPage ? maxPage : prev;
        });
    }, [noticeTotalPages]);

    const resetEditor = () => {
        setNoticeForm(INITIAL_NOTICE_FORM);
        setEditingNoticeId(null);
    };

    const startNoticeEdit = (notice) => {
        setEditingNoticeId(notice.noticeId);
        setNoticeForm({
            category: notice.category,
            title: notice.title,
            body: notice.body
        });

        requestAnimationFrame(() => {
            noticeEditorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            noticeTitleInputRef.current?.focus();
        });
    };

    const handleSubmitNotice = async () => {
        if (!noticeForm.title.trim() || !noticeForm.body.trim() || !noticeForm.category.trim()) {
            alert('카테고리, 제목, 내용을 모두 입력해주세요.');
            return;
        }
        setSaving(true);
        try {
            if (editingNoticeId) {
                await axios.put(
                    `${process.env.REACT_APP_FUNDINGBOOST}/admin/notices/${editingNoticeId}`,
                    noticeForm,
                    authConfig
                );
            } else {
                await axios.post(
                    `${process.env.REACT_APP_FUNDINGBOOST}/admin/notices`,
                    noticeForm,
                    authConfig
                );
            }
            await loadNotices();
            resetEditor();
        } catch (error) {
            console.error('공지 저장 실패', error);
            alert('공지 저장에 실패했습니다.');
        } finally {
            setSaving(false);
        }
    };

    const handleDeleteNotice = async (noticeId) => {
        const confirmed = window.confirm('해당 공지사항을 삭제하시겠습니까?');
        if (!confirmed) {
            return;
        }
        try {
            await axios.delete(`${process.env.REACT_APP_FUNDINGBOOST}/admin/notices/${noticeId}`, authConfig);
            await loadNotices();
            if (editingNoticeId === noticeId) {
                resetEditor();
            }
        } catch (error) {
            console.error('공지 삭제 실패', error);
            alert('공지 삭제에 실패했습니다.');
        }
    };

    return (
        <div className="mypageNoticePane">
            {modalShowState && <NonMemberModal message="로그인 후 펀딩부스트를 시작해보세요." />}
            <div className="mypageNoticePane-left">
                <MypageProfile profileInfo={profileData || {}} />
                <MyPageIndex onButtonClick={handleButtonClick} currentPageIndex={7} />
            </div>
            <div className="mypageNoticePane-right">
                <div className="mypageNoticePane-card">
                    <div className="mypageNoticePane-header">
                        <div>
                            <div className="mypageNoticePane-eyebrow">Notice</div>
                            <h2>공지사항</h2>
                        </div>
                        <p>서비스 변경사항과 운영 공지를 확인할 수 있습니다.</p>
                    </div>
                    {isAdmin && (
                        <div className="mypageNoticeAdminEditor" ref={noticeEditorRef}>
                            <h3>{editingNoticeId ? '공지 수정' : '공지 작성'}</h3>
                            <div className="mypageNoticeAdminGrid">
                                <select
                                    value={noticeForm.category}
                                    onChange={(event) => setNoticeForm((prev) => ({ ...prev, category: event.target.value }))}
                                >
                                    <option value="안내">안내</option>
                                    <option value="업데이트">업데이트</option>
                                    <option value="점검">점검</option>
                                    <option value="이벤트">이벤트</option>
                                    <option value="운영">운영</option>
                                </select>
                                <input
                                    type="text"
                                    placeholder="공지 제목"
                                    value={noticeForm.title}
                                    ref={noticeTitleInputRef}
                                    onChange={(event) => setNoticeForm((prev) => ({ ...prev, title: event.target.value }))}
                                />
                            </div>
                            <textarea
                                rows={4}
                                placeholder="공지 내용을 입력하세요."
                                value={noticeForm.body}
                                onChange={(event) => setNoticeForm((prev) => ({ ...prev, body: event.target.value }))}
                            />
                            <div className="mypageNoticeAdminActions">
                                {editingNoticeId && (
                                    <button type="button" onClick={resetEditor} className="secondary">
                                        수정 취소
                                    </button>
                                )}
                                <button type="button" onClick={handleSubmitNotice} disabled={saving}>
                                    {saving ? '저장 중...' : editingNoticeId ? '수정 저장' : '공지 등록'}
                                </button>
                            </div>
                        </div>
                    )}
                    <div className="mypageNoticePane-list">
                        {loading && <p className="mypageNoticeEmpty">공지사항을 불러오는 중입니다...</p>}
                        {!loading && errorMessage && <p className="mypageNoticeEmpty">{errorMessage}</p>}
                        {!loading && !errorMessage && notices.length === 0 && (
                            <p className="mypageNoticeEmpty">등록된 공지사항이 없습니다.</p>
                        )}
                        {!loading && !errorMessage && pagedNotices.map((notice) => (
                            <article key={notice.noticeId} className="mypageNoticeItem">
                                <div className="mypageNoticeItem-meta">
                                    <span className={`mypageNoticeBadge ${notice.category === '업데이트' ? 'accent' : ''}`}>
                                        {notice.category}
                                    </span>
                                    <span className="mypageNoticeDate">{formatDate(notice.createdDate)}</span>
                                </div>
                                <h3>{notice.title}</h3>
                                <p>{notice.body}</p>
                                {isAdmin && (
                                    <div className="mypageNoticeItemActions">
                                        <button
                                            type="button"
                                            onClick={() => startNoticeEdit(notice)}
                                        >
                                            수정
                                        </button>
                                        <button type="button" className="danger" onClick={() => handleDeleteNotice(notice.noticeId)}>
                                            삭제
                                        </button>
                                    </div>
                                )}
                            </article>
                        ))}
                    </div>
                    {!loading && !errorMessage && notices.length > NOTICE_PAGE_SIZE && (
                        <div className="mypageNoticePagination">
                            <button
                                type="button"
                                onClick={() => setNoticePage((prev) => Math.max(prev - 1, 0))}
                                disabled={noticePage <= 0}
                            >
                                이전
                            </button>
                            <span>{noticePage + 1} / {noticeTotalPages}</span>
                            <button
                                type="button"
                                onClick={() => setNoticePage((prev) => Math.min(prev + 1, noticeTotalPages - 1))}
                                disabled={noticePage >= noticeTotalPages - 1}
                            >
                                다음
                            </button>
                        </div>
                    )}
                </div>
            </div>
        </div>
    );
};

export default MypageNoticePane;
