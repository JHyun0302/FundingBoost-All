import React, {useEffect, useState} from 'react';
import './mypage-wishlist.scss';
import MypageIndex from '../../../molecules/MypageIndex/mypageindex';
import MypageProfile from '../../../molecules/MypageProfile/mypageprofile';
import MyPageIndex from "../../../molecules/MypageIndex/mypageindex";
import axios from "axios";
import WishList from "../../../molecules/wishList/wishList";
import NonMemberModal from "../../../atoms/nonMemberModal/nonMemberModal";

const getBookmarkCategory = (bookmarkItem) => {
    const candidates = [
        bookmarkItem?.category,
        bookmarkItem?.itemCategory,
        bookmarkItem?.categoryName,
        bookmarkItem?.item_category
    ];
    const category = candidates.find((value) => typeof value === "string" && value.trim().length > 0);
    return category ? category.trim() : "";
};

const WishListPane = () => {
    const [apiData, setApiData] = useState(null);
    const [modalShowState, setModalShowState] = useState(false);
    const [isRemoving, setIsRemoving] = useState(false);

    const handleButtonClick = (index) => {
        console.log(`Selected index: ${index}`);
    };

    useEffect(() => {
        const enrichBookmarkCategories = async (bookmarkData, accessToken) => {
            const bookmarkItems = bookmarkData?.bookmarkItemDtos;
            if (!Array.isArray(bookmarkItems) || bookmarkItems.length === 0) {
                return bookmarkData;
            }

            const missingCategoryItems = bookmarkItems.filter((bookmarkItem) =>
                !getBookmarkCategory(bookmarkItem) && bookmarkItem?.itemId != null
            );

            if (missingCategoryItems.length === 0) {
                return bookmarkData;
            }

            const apiV3Base = process.env.REACT_APP_FUNDINGBOOST_V3 || "/api/v3";
            const apiV1Base = process.env.REACT_APP_FUNDINGBOOST || "/api/v1";
            const headers = accessToken
                ? { Authorization: `Bearer ${accessToken}` }
                : {};

            const fetchCategory = async (itemId) => {
                const urls = [
                    `${apiV3Base}/items/${itemId}`,
                    `${apiV1Base}/items/${itemId}`
                ];

                for (const url of urls) {
                    try {
                        const response = await axios({
                            method: 'GET',
                            url,
                            headers,
                            responseType: 'json'
                        });
                        const fetchedCategory = response?.data?.data?.category;
                        if (typeof fetchedCategory === "string" && fetchedCategory.trim().length > 0) {
                            return fetchedCategory.trim();
                        }
                    } catch (_error) {
                        // 다음 엔드포인트로 재시도
                    }
                }

                return null;
            };

            const categoryEntries = await Promise.all(
                missingCategoryItems.map(async (bookmarkItem) => {
                    const fetchedCategory = await fetchCategory(bookmarkItem.itemId);
                    if (!fetchedCategory) {
                        return null;
                    }
                    return [bookmarkItem.itemId, fetchedCategory];
                })
            );

            const categoryMap = new Map(categoryEntries.filter(Boolean));
            if (categoryMap.size === 0) {
                return bookmarkData;
            }

            return {
                ...bookmarkData,
                bookmarkItemDtos: bookmarkItems.map((bookmarkItem) => {
                    const existingCategory = getBookmarkCategory(bookmarkItem);
                    if (existingCategory) {
                        return {
                            ...bookmarkItem,
                            category: existingCategory
                        };
                    }
                    const mappedCategory = categoryMap.get(bookmarkItem.itemId);
                    if (!mappedCategory) {
                        return bookmarkItem;
                    }
                    return {
                        ...bookmarkItem,
                        category: mappedCategory
                    };
                })
            };
        };

        // API 호출 함수
        const fetchData = async () => {
            try {
                const accessToken = localStorage.getItem('accessToken');
                if (!accessToken) {
                    setModalShowState(true);
                    return;
                }

                const response = await axios({
                    method: 'GET',
                    url: `${process.env.REACT_APP_FUNDINGBOOST}/bookmark`,
                    headers: {
                        "Authorization": `Bearer ${accessToken}`,
                    },
                    responseType: 'json'
                });
                console.log(response.data); // 콘솔에 데이터 출력
                const bookmarkData = response.data.data;
                setApiData(bookmarkData); // 상태에 데이터 저장

                const enrichedData = await enrichBookmarkCategories(bookmarkData, accessToken);
                setApiData(enrichedData);

            } catch (error) {
                console.error("API 호출 중 오류가 발생했습니다.", error);
            }
        };

        fetchData();
    }, []);

    const handleRemoveBookmark = async (itemId) => {
        if (isRemoving) {
            return;
        }
        const confirmed = window.confirm("위시리스트에서 해당 상품을 삭제할까요?");
        if (!confirmed) {
            return;
        }

        try {
            const accessToken = localStorage.getItem('accessToken');
            if (!accessToken) {
                setModalShowState(true);
                return;
            }

            setIsRemoving(true);
            await axios({
                method: 'POST',
                url: `${process.env.REACT_APP_FUNDINGBOOST}/bookmark/like/${itemId}`,
                headers: {
                    "Authorization": `Bearer ${accessToken}`,
                },
                responseType: 'json'
            });

            setApiData((prev) => {
                if (!prev) {
                    return prev;
                }

                return {
                    ...prev,
                    bookmarkItemDtos: (prev.bookmarkItemDtos || []).filter((bookmarkItem) => bookmarkItem.itemId !== itemId)
                };
            });
        } catch (error) {
            console.error("위시리스트 삭제 중 오류가 발생했습니다.", error);
        } finally {
            setIsRemoving(false);
        }
    };

    return (
        <div className="mypage-myhistory-total-container">
            {modalShowState && <NonMemberModal message="로그인 후 펀딩부스트를 시작해보세요." />}
            <div className="mypage-myhistory-left-pane-container">
                <MypageProfile profileInfo={apiData || {}} />
                <MyPageIndex onButtonClick={handleButtonClick} currentPageIndex={5} />
            </div>
            <div className="mypage-myhistory-right-pane-containter">
               <WishList wishListData={apiData} onRemoveBookmark={handleRemoveBookmark} isRemoving={isRemoving} />
            </div>
        </div>
    );
}

export default WishListPane;
