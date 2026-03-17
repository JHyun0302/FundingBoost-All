import React, { useEffect, useMemo, useState } from "react";
import { useNavigate, useParams } from "react-router-dom";
import axios from "axios";
import "./shopping-detail.scss";
import ShoppingDetailMenu from "../../../atoms/Shopping-Detail-Menu/shopping-detail-menu";
import nonItemImg from "../../../../assets/nonItemImg.svg";
import { toImageProxyUrl } from "../../../../utils/imageProxyUrl";

const priceFormatter = new Intl.NumberFormat("ko-KR");

const getStoredAccessToken = () => {
    const token = localStorage.getItem("accessToken");
    if (!token) {
        return "";
    }

    const trimmedToken = token.trim();
    if (!trimmedToken || trimmedToken === "null" || trimmedToken === "undefined") {
        return "";
    }

    try {
        const [, payload] = trimmedToken.split(".");
        if (!payload) {
            localStorage.removeItem("accessToken");
            localStorage.removeItem("refreshToken");
            return "";
        }

        const normalizedPayload = payload.replace(/-/g, "+").replace(/_/g, "/");
        const decodedPayload = JSON.parse(atob(normalizedPayload));
        if (decodedPayload?.exp && Date.now() >= decodedPayload.exp * 1000) {
            localStorage.removeItem("accessToken");
            localStorage.removeItem("refreshToken");
            return "";
        }
    } catch (error) {
        localStorage.removeItem("accessToken");
        localStorage.removeItem("refreshToken");
        return "";
    }

    return trimmedToken;
};

const buildHeaders = () => {
    const accessToken = getStoredAccessToken();
    const headers = {
        "Content-Type": "application/json"
    };

    if (accessToken) {
        headers.Authorization = `Bearer ${accessToken}`;
    }

    return headers;
};

const formatPrice = (price) => {
    if (typeof price !== "number") {
        return "0";
    }
    return priceFormatter.format(price);
};

const DETAIL_TABS = [
    { key: "info", label: "상품정보" },
    { key: "review", label: "리뷰 안내" },
    { key: "policy", label: "문의/정책" }
];

const RELATED_SEARCH_HINTS = [
    "맥북", "macbook", "노트북", "laptop", "그램", "갤럭시북",
    "아이폰", "iphone", "갤럭시", "스마트폰", "휴대폰",
    "보조배터리", "충전기", "키보드", "마우스", "이어폰", "헤드폰",
    "가방", "지갑", "목걸이", "반지", "팔찌", "텀블러", "머그",
    "고기", "한우", "화장품", "향수", "립스틱", "과자", "초콜릿"
];

const RELATED_TOKEN_STOPWORDS = new Set([
    "선물", "추천", "단독", "신제품", "신상", "정품", "공식", "프리미엄",
    "특가", "할인", "기획", "행사", "이벤트", "증정", "한정", "예약",
    "배송", "무료", "세트", "구성", "대용량", "국내", "해외", "당일"
]);

const deriveRelatedKeyword = (itemData) => {
    if (!itemData) {
        return "";
    }

    const normalizedName = typeof itemData.itemName === "string" ? itemData.itemName.toLowerCase() : "";
    for (const hint of RELATED_SEARCH_HINTS) {
        if (normalizedName.includes(hint)) {
            return hint;
        }
    }

    const brandName = typeof itemData.brandName === "string" ? itemData.brandName.trim() : "";
    if (brandName.length >= 2) {
        return brandName;
    }

    const tokens = normalizedName.match(/[0-9a-zA-Z가-힣]{2,}/g) || [];
    for (const token of tokens) {
        if (RELATED_TOKEN_STOPWORDS.has(token) || /^\d+$/.test(token)) {
            continue;
        }
        return token;
    }

    return "";
};

const ShoppingDetailPane = () => {
    const navigate = useNavigate();
    const { itemId } = useParams();
    const apiV3Base = process.env.REACT_APP_FUNDINGBOOST_V3 || "/api/v3";

    const [itemData, setItemData] = useState(null);
    const [relatedItems, setRelatedItems] = useState([]);
    const [loading, setLoading] = useState(true);
    const [relatedLoading, setRelatedLoading] = useState(false);
    const [errorMessage, setErrorMessage] = useState("");
    const [activeTab, setActiveTab] = useState("info");
    const [selectedImageIndex, setSelectedImageIndex] = useState(0);

    const normalizedOptions = useMemo(() => {
        const optionValues = itemData?.options;
        if (!Array.isArray(optionValues)) {
            return [];
        }

        return optionValues
            .map((value) => (value || "").trim())
            .filter((value) => value.length > 0)
            .filter((value, index, array) => array.indexOf(value) === index);
    }, [itemData?.options]);

    const imageCandidates = useMemo(() => {
        const src = toImageProxyUrl(itemData?.itemThumbnailImageUrl || nonItemImg, {
            width: 960,
            height: 960,
            quality: 85
        });
        return [src, src, src];
    }, [itemData?.itemThumbnailImageUrl]);

    useEffect(() => {
        let mounted = true;

        const fetchFundingItemData = async () => {
            setLoading(true);
            setErrorMessage("");
            setRelatedItems([]);

            try {
                const headers = buildHeaders();
                const response = await axios.get(`${apiV3Base}/items/${itemId}`, {
                    responseType: "json",
                    headers
                });

                if (!mounted) {
                    return;
                }

                setItemData(response?.data?.data ?? null);
                setSelectedImageIndex(0);
            } catch (error) {
                if (!mounted) {
                    return;
                }
                console.error("GET 에러:", error);
                setErrorMessage("상품 정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
            } finally {
                if (mounted) {
                    setLoading(false);
                }
            }
        };

        fetchFundingItemData();

        return () => {
            mounted = false;
        };
    }, [apiV3Base, itemId]);

    useEffect(() => {
        let mounted = true;

        const fetchRelatedItems = async () => {
            const category = itemData?.category;
            if (!category) {
                setRelatedItems([]);
                return;
            }

            setRelatedLoading(true);

            try {
                const headers = buildHeaders();
                const currentItemId = Number(itemId);
                const normalizedCategory = typeof category === "string" ? category.trim() : "";
                const relatedById = new Map();

                const addRelatedItems = (items) => {
                    if (!Array.isArray(items)) {
                        return;
                    }

                    for (const product of items) {
                        const productId = Number(product?.itemId);
                        if (!Number.isFinite(productId) || productId === currentItemId) {
                            continue;
                        }

                        const productCategory = typeof product?.category === "string" ? product.category.trim() : "";
                        if (normalizedCategory && productCategory !== normalizedCategory) {
                            continue;
                        }

                        if (!relatedById.has(productId)) {
                            relatedById.set(productId, product);
                        }

                        if (relatedById.size >= 8) {
                            break;
                        }
                    }
                };

                const relatedKeyword = deriveRelatedKeyword(itemData);
                if (relatedKeyword.length >= 2) {
                    const keywordResponse = await axios.get(`${apiV3Base}/search`, {
                        params: {
                            keyword: relatedKeyword,
                            page: 0,
                            size: 24
                        },
                        headers
                    });
                    addRelatedItems(keywordResponse?.data?.data?.content);
                }

                if (relatedById.size < 8) {
                    const categoryResponse = await axios.get(`${apiV3Base}/items`, {
                        params: {
                            category,
                            page: 0,
                            size: 24
                        },
                        headers
                    });
                    addRelatedItems(categoryResponse?.data?.data?.content);
                }

                if (!mounted) {
                    return;
                }

                setRelatedItems(Array.from(relatedById.values()).slice(0, 8));
            } catch (error) {
                if (mounted) {
                    console.error("관련 상품 조회 실패:", error);
                    setRelatedItems([]);
                }
            } finally {
                if (mounted) {
                    setRelatedLoading(false);
                }
            }
        };

        fetchRelatedItems();

        return () => {
            mounted = false;
        };
    }, [apiV3Base, itemData, itemId]);

    const handleRelatedClick = (targetItemId) => {
        navigate(`/shopping/detail/${targetItemId}`);
        window.scrollTo({ top: 0, behavior: "smooth" });
    };

    const renderTabContent = () => {
        if (!itemData) {
            return null;
        }

        if (activeTab === "review") {
            return (
                <div className="shopping-detail-placeholder-card">
                    <h3>리뷰 데이터 안내</h3>
                    <p>
                        현재 토이 프로젝트에서는 상품 리뷰 원문을 별도 수집하지 않습니다.
                        추후 크롤러 확장 시 평점/리뷰 개수/대표 리뷰를 추가할 수 있습니다.
                    </p>
                    <ul>
                        <li>현재 제공 데이터: 상품명, 가격, 브랜드, 카테고리, 옵션</li>
                        <li>리뷰 기능은 주문 완료 후 MY 리뷰 탭에서 작성/조회 가능합니다.</li>
                    </ul>
                </div>
            );
        }

        if (activeTab === "policy") {
            return (
                <div className="shopping-detail-policy-grid">
                    <article>
                        <h4>배송 안내</h4>
                        <p>7만원 이상 구매 시 무료배송, 3,000원 우체국택배</p>
                        <p>영업일 기준 2~3일 내 도착 예정</p>
                    </article>
                    <article>
                        <h4>교환/환불 정책</h4>
                        <p>단순 변심: 수령 후 7일 이내 접수 가능</p>
                        <p>상품 하자/오배송: 사진 첨부 후 고객센터로 문의</p>
                    </article>
                    <article>
                        <h4>문의 방법</h4>
                        <p>마이페이지 &gt; 고객센터에서 FAQ 확인 및 문의 등록</p>
                        <p>주문번호와 상품명을 함께 남기면 처리가 빠릅니다.</p>
                    </article>
                </div>
            );
        }

        return (
            <div className="shopping-detail-info-grid">
                <dl>
                    <dt>상품명</dt>
                    <dd>{itemData.itemName}</dd>
                </dl>
                <dl>
                    <dt>브랜드</dt>
                    <dd>{itemData.brandName || "-"}</dd>
                </dl>
                <dl>
                    <dt>카테고리</dt>
                    <dd>{itemData.category || "-"}</dd>
                </dl>
                <dl>
                    <dt>판매가</dt>
                    <dd>{formatPrice(itemData.itemPrice)}원</dd>
                </dl>
                <dl>
                    <dt>옵션 개수</dt>
                    <dd>{normalizedOptions.length > 0 ? `${normalizedOptions.length}개` : "없음"}</dd>
                </dl>
                <dl>
                    <dt>대표 옵션</dt>
                    <dd>
                        {normalizedOptions.length > 0 ? normalizedOptions.slice(0, 4).join(" / ") : "옵션 정보 없음"}
                    </dd>
                </dl>
                <dl>
                    <dt>찜 상태</dt>
                    <dd>{itemData.bookmark ? "위시리스트에 담김" : "미등록"}</dd>
                </dl>
            </div>
        );
    };

    if (loading) {
        return <div className="shopping-detail-state">상품 정보를 불러오는 중입니다.</div>;
    }

    if (errorMessage) {
        return <div className="shopping-detail-state error">{errorMessage}</div>;
    }

    if (!itemData) {
        return <div className="shopping-detail-state">조회 가능한 상품이 없습니다.</div>;
    }

    return (
        <div className="shopping-detail-container">
            <section className="shopping-detail-hero">
                <div className="shopping-detail-media-panel">
                    <div className="shopping-detail-main-image-wrapper">
                        <img
                            src={imageCandidates[selectedImageIndex] || nonItemImg}
                            alt={itemData.itemName}
                            onError={(event) => {
                                event.currentTarget.onerror = null;
                                event.currentTarget.src = nonItemImg;
                            }}
                        />
                    </div>
                    <div className="shopping-detail-thumbnail-row">
                        {imageCandidates.map((imageUrl, index) => (
                            <button
                                type="button"
                                key={`${imageUrl}-${index}`}
                                className={index === selectedImageIndex ? "active" : ""}
                                onClick={() => setSelectedImageIndex(index)}
                                aria-label={`${index + 1}번째 상품 이미지 보기`}
                            >
                                <img
                                    src={imageUrl}
                                    alt={`${itemData.itemName} 썸네일 ${index + 1}`}
                                    onError={(event) => {
                                        event.currentTarget.onerror = null;
                                        event.currentTarget.src = nonItemImg;
                                    }}
                                />
                            </button>
                        ))}
                    </div>
                </div>

                <div className="shopping-detail-summary-panel">
                    <div className="shopping-detail-meta-chip-wrap">
                        <span className="chip brand">{itemData.brandName || "브랜드 정보 없음"}</span>
                        <span className="chip category">{itemData.category || "카테고리 미분류"}</span>
                    </div>
                    <h1>{itemData.itemName}</h1>
                    <div className="shopping-detail-price-line">
                        <span>판매가</span>
                        <strong>{formatPrice(itemData.itemPrice)}원</strong>
                    </div>

                    <ShoppingDetailMenu
                        itemId={itemId}
                        itemName={itemData.itemName}
                        itemThumbnailImageUrl={itemData.itemThumbnailImageUrl}
                        itemPrice={itemData.itemPrice}
                        options={itemData.options}
                        bookmark={itemData.bookmark}
                    />
                </div>
            </section>

            <section className="shopping-detail-tab-section">
                <nav className="shopping-detail-tab-nav" aria-label="상세 탭">
                    {DETAIL_TABS.map((tab) => (
                        <button
                            type="button"
                            key={tab.key}
                            className={tab.key === activeTab ? "active" : ""}
                            onClick={() => setActiveTab(tab.key)}
                        >
                            {tab.label}
                        </button>
                    ))}
                </nav>
                <div className="shopping-detail-tab-panel">{renderTabContent()}</div>
            </section>

            <section className="shopping-detail-related-section">
                <div className="shopping-detail-related-header">
                    <h2>같은 카테고리 추천 상품</h2>
                    <p>현재 상품과 같은 카테고리에서 인기 상품을 모아봤어요.</p>
                </div>

                {relatedLoading ? (
                    <div className="shopping-detail-empty">추천 상품을 불러오는 중입니다.</div>
                ) : relatedItems.length === 0 ? (
                    <div className="shopping-detail-empty">추천할 수 있는 같은 카테고리 상품이 없습니다.</div>
                ) : (
                    <div className="shopping-detail-related-grid">
                        {relatedItems.map((relatedItem) => (
                            <article
                                key={relatedItem.itemId}
                                className="shopping-detail-related-card"
                                onClick={() => handleRelatedClick(relatedItem.itemId)}
                                role="button"
                                tabIndex={0}
                                onKeyDown={(event) => {
                                    if (event.key === "Enter" || event.key === " ") {
                                        event.preventDefault();
                                        handleRelatedClick(relatedItem.itemId);
                                    }
                                }}
                            >
                                <div className="thumbnail-wrap">
                                    <img
                                        src={toImageProxyUrl(relatedItem.itemImageUrl || nonItemImg, {
                                            width: 320,
                                            height: 320,
                                            quality: 80
                                        })}
                                        alt={relatedItem.itemName}
                                        onError={(event) => {
                                            event.currentTarget.onerror = null;
                                            event.currentTarget.src = nonItemImg;
                                        }}
                                    />
                                </div>
                                <p className="brand">{relatedItem.brandName || "브랜드"}</p>
                                <h3>{relatedItem.itemName}</h3>
                                <strong>{formatPrice(relatedItem.price)}원</strong>
                            </article>
                        ))}
                    </div>
                )}
            </section>
        </div>
    );
};

export default ShoppingDetailPane;
