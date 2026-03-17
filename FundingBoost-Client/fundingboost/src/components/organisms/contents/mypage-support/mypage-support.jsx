import React, { useEffect, useMemo, useRef, useState } from 'react';
import axios from 'axios';
import './mypage-support.scss';
import MypageProfile from '../../../molecules/MypageProfile/mypageprofile';
import MyPageIndex from '../../../molecules/MypageIndex/mypageindex';
import NonMemberModal from '../../../atoms/nonMemberModal/nonMemberModal';

const INITIAL_FAQ_FORM = {
    question: '',
    answer: '',
    sortOrder: 1
};
const FAQ_PAGE_SIZE = 5;

const MypageSupportPane = () => {
    const [profileData, setProfileData] = useState(null);
    const [modalShowState, setModalShowState] = useState(false);
    const [faqItems, setFaqItems] = useState([]);
    const [openFaqId, setOpenFaqId] = useState(null);
    const [loading, setLoading] = useState(true);
    const [errorMessage, setErrorMessage] = useState('');
    const [isAdmin, setIsAdmin] = useState(false);
    const [faqForm, setFaqForm] = useState(INITIAL_FAQ_FORM);
    const [editingFaqId, setEditingFaqId] = useState(null);
    const [saving, setSaving] = useState(false);
    const [faqPage, setFaqPage] = useState(0);
    const faqEditorRef = useRef(null);
    const faqQuestionInputRef = useRef(null);

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

    const loadFaqs = async () => {
        const response = await axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/support/faqs`, authConfig);
        const nextFaqs = response?.data?.data?.faqs || [];
        setFaqItems(nextFaqs);
    };

    const faqTotalPages = useMemo(
        () => Math.max(Math.ceil(faqItems.length / FAQ_PAGE_SIZE), 1),
        [faqItems.length]
    );

    const pagedFaqItems = useMemo(() => {
        const start = faqPage * FAQ_PAGE_SIZE;
        return faqItems.slice(start, start + FAQ_PAGE_SIZE);
    }, [faqItems, faqPage]);

    const maxSortOrder = useMemo(() => {
        const baseMax = editingFaqId ? faqItems.length : faqItems.length + 1;
        return Math.max(baseMax, 1);
    }, [editingFaqId, faqItems.length]);

    useEffect(() => {
        const fetchData = async () => {
            try {
                if (!accessToken) {
                    setModalShowState(true);
                    setLoading(false);
                    return;
                }

                const [profileResponse, faqResponse, accessResponse] = await Promise.all([
                    axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/funding/my-funding-status`, authConfig),
                    axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/support/faqs`, authConfig),
                    axios.get(`${process.env.REACT_APP_FUNDINGBOOST}/admin/dashboard/access`, authConfig).catch(() => null)
                ]);
                setProfileData(profileResponse?.data?.data || null);

                const nextFaqs = faqResponse?.data?.data?.faqs || [];
                setFaqItems(nextFaqs);
                setOpenFaqId(nextFaqs.length > 0 ? nextFaqs[0].faqId : null);
                setIsAdmin(accessResponse?.data?.data?.isSuccess === true);
                setErrorMessage('');
            } catch (error) {
                console.error('고객센터 정보를 불러오는 중 오류가 발생했습니다.', error);
                setErrorMessage('고객센터 FAQ를 불러오지 못했습니다.');
            } finally {
                setLoading(false);
            }
        };

        fetchData();
    }, [accessToken]);

    useEffect(() => {
        setFaqPage((prev) => {
            const maxPage = Math.max(faqTotalPages - 1, 0);
            return prev > maxPage ? maxPage : prev;
        });
    }, [faqTotalPages]);

    useEffect(() => {
        if (pagedFaqItems.length === 0) {
            setOpenFaqId(null);
            return;
        }
        const existsInPage = pagedFaqItems.some((faq) => faq.faqId === openFaqId);
        if (!existsInPage) {
            setOpenFaqId(pagedFaqItems[0].faqId);
        }
    }, [pagedFaqItems, openFaqId]);

    const toggleFaq = (faqId) => {
        setOpenFaqId((prev) => (prev === faqId ? null : faqId));
    };

    const resetEditor = () => {
        setFaqForm(INITIAL_FAQ_FORM);
        setEditingFaqId(null);
    };

    const startFaqEdit = (faq) => {
        setEditingFaqId(faq.faqId);
        setFaqForm({
            question: faq.question,
            answer: faq.answer,
            sortOrder: Math.max(faq.sortOrder || 1, 1)
        });
        setOpenFaqId(faq.faqId);
        requestAnimationFrame(() => {
            faqEditorRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
            faqQuestionInputRef.current?.focus();
        });
    };

    const handleSubmitFaq = async () => {
        if (!faqForm.question.trim() || !faqForm.answer.trim()) {
            alert('질문과 답변을 모두 입력해주세요.');
            return;
        }

        const sortOrder = Number(faqForm.sortOrder);
        if (!Number.isInteger(sortOrder) || sortOrder < 1 || sortOrder > maxSortOrder) {
            alert(`정렬순서는 1부터 ${maxSortOrder}까지만 입력할 수 있습니다.`);
            return;
        }

        setSaving(true);
        try {
            const requestBody = {
                question: faqForm.question,
                answer: faqForm.answer,
                sortOrder
            };

            if (editingFaqId) {
                await axios.put(
                    `${process.env.REACT_APP_FUNDINGBOOST}/admin/support/faqs/${editingFaqId}`,
                    requestBody,
                    authConfig
                );
            } else {
                await axios.post(
                    `${process.env.REACT_APP_FUNDINGBOOST}/admin/support/faqs`,
                    requestBody,
                    authConfig
                );
            }

            await loadFaqs();
            resetEditor();
        } catch (error) {
            console.error('FAQ 저장 실패', error);
            alert('FAQ 저장에 실패했습니다.');
        } finally {
            setSaving(false);
        }
    };

    const handleDeleteFaq = async (faqId) => {
        const confirmed = window.confirm('해당 FAQ를 삭제하시겠습니까?');
        if (!confirmed) {
            return;
        }
        try {
            await axios.delete(`${process.env.REACT_APP_FUNDINGBOOST}/admin/support/faqs/${faqId}`, authConfig);
            await loadFaqs();
            if (editingFaqId === faqId) {
                resetEditor();
            }
        } catch (error) {
            console.error('FAQ 삭제 실패', error);
            alert('FAQ 삭제에 실패했습니다.');
        }
    };

    return (
        <div className="mypageSupportPane">
            {modalShowState && <NonMemberModal message="로그인 후 펀딩부스트를 시작해보세요." />}
            <div className="mypageSupportPane-left">
                <MypageProfile profileInfo={profileData || {}} />
                <MyPageIndex onButtonClick={handleButtonClick} currentPageIndex={8} />
            </div>
            <div className="mypageSupportPane-right">
                <div className="mypageSupportPane-card">
                    <div className="mypageSupportPane-header">
                        <div>
                            <div className="mypageSupportPane-eyebrow">Support</div>
                            <h2>고객센터</h2>
                        </div>
                        <p>자주 묻는 질문을 확인하고 운영자가 FAQ를 직접 관리할 수 있습니다.</p>
                    </div>

                    <div className="mypageSupportContact">
                        <div className="mypageSupportContactTitle">문의 채널</div>
                        <div className="mypageSupportContactValue">support@fundingboost.test</div>
                        <div className="mypageSupportContactHint">버그 제보, 기능 문의, 테스트 계정 이슈를 메일로 접수할 수 있습니다.</div>
                    </div>

                    {isAdmin && (
                        <div className="mypageSupportAdminEditor" ref={faqEditorRef}>
                            <h3>{editingFaqId ? 'FAQ 수정' : 'FAQ 작성'}</h3>
                            <input
                                type="text"
                                placeholder="질문"
                                value={faqForm.question}
                                ref={faqQuestionInputRef}
                                onChange={(event) => setFaqForm((prev) => ({ ...prev, question: event.target.value }))}
                            />
                            <textarea
                                rows={4}
                                placeholder="답변"
                                value={faqForm.answer}
                                onChange={(event) => setFaqForm((prev) => ({ ...prev, answer: event.target.value }))}
                            />
                            <div className="mypageSupportAdminActions">
                                <label htmlFor="faq-sort-order">정렬순서</label>
                                <input
                                    id="faq-sort-order"
                                    type="number"
                                    min={1}
                                    max={maxSortOrder}
                                    value={faqForm.sortOrder}
                                    onChange={(event) => {
                                        const raw = event.target.value;
                                        if (raw === '') {
                                            setFaqForm((prev) => ({ ...prev, sortOrder: '' }));
                                            return;
                                        }
                                        const parsed = Number(raw);
                                        if (!Number.isFinite(parsed)) {
                                            return;
                                        }
                                        const bounded = Math.max(1, Math.min(parsed, maxSortOrder));
                                        setFaqForm((prev) => ({ ...prev, sortOrder: bounded }));
                                    }}
                                />
                                <span className="mypageSupportSortHint">1 ~ {maxSortOrder}</span>
                                <span className="mypageSupportSortHint">
                                    입력한 순서에 삽입되며 기존 FAQ는 자동으로 뒤로 이동합니다.
                                </span>
                                {editingFaqId && (
                                    <button type="button" className="secondary" onClick={resetEditor}>수정 취소</button>
                                )}
                                <button type="button" onClick={handleSubmitFaq} disabled={saving}>
                                    {saving ? '저장 중...' : editingFaqId ? '수정 저장' : 'FAQ 등록'}
                                </button>
                            </div>
                        </div>
                    )}

                    <div className="mypageSupportFaqList">
                        {loading && <p className="mypageSupportEmpty">FAQ를 불러오는 중입니다...</p>}
                        {!loading && errorMessage && <p className="mypageSupportEmpty">{errorMessage}</p>}
                        {!loading && !errorMessage && faqItems.length === 0 && (
                            <p className="mypageSupportEmpty">등록된 FAQ가 없습니다.</p>
                        )}
                        {!loading && !errorMessage && pagedFaqItems.map((faq) => {
                            const isOpen = openFaqId === faq.faqId;

                            return (
                                <div className={`mypageSupportFaqItem ${isOpen ? 'open' : ''}`} key={faq.faqId}>
                                    <button
                                        type="button"
                                        className="mypageSupportFaqQuestion"
                                        onClick={() => toggleFaq(faq.faqId)}
                                    >
                                        <span>{faq.question}</span>
                                        <span className="mypageSupportFaqToggle">{isOpen ? '−' : '+'}</span>
                                    </button>
                                    {isOpen && (
                                        <div className="mypageSupportFaqAnswer">
                                            {faq.answer}
                                            {isAdmin && (
                                                <div className="mypageSupportFaqActions">
                                                    <button
                                                        type="button"
                                                        onClick={() => startFaqEdit(faq)}
                                                    >
                                                        수정
                                                    </button>
                                                    <button
                                                        type="button"
                                                        className="danger"
                                                        onClick={() => handleDeleteFaq(faq.faqId)}
                                                    >
                                                        삭제
                                                    </button>
                                                </div>
                                            )}
                                        </div>
                                    )}
                                </div>
                            );
                        })}
                    </div>
                    {!loading && !errorMessage && faqItems.length > FAQ_PAGE_SIZE && (
                        <div className="mypageSupportPagination">
                            <button
                                type="button"
                                onClick={() => setFaqPage((prev) => Math.max(prev - 1, 0))}
                                disabled={faqPage <= 0}
                            >
                                이전
                            </button>
                            <span>{faqPage + 1} / {faqTotalPages}</span>
                            <button
                                type="button"
                                onClick={() => setFaqPage((prev) => Math.min(prev + 1, faqTotalPages - 1))}
                                disabled={faqPage >= faqTotalPages - 1}
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

export default MypageSupportPane;
