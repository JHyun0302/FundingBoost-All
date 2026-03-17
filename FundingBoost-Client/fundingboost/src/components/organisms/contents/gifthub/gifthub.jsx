import React, { useEffect, useMemo, useState } from 'react';
import '../gifthub/gifthub.scss';
import SingleGiftHubItem from '../../../molecules/SingleGifthubItem/singlegifthubitem';
import GifthubResult from '../../../molecules/GifthubResult/gifthubresult';
import axios from 'axios';
import NonMemberModal from '../../../atoms/nonMemberModal/nonMemberModal';
import GifthubNonItem from '../../../organisms/contents/gifthubNonItem/gifthubNonItem';

const GifthubPane = () => {
    const [items, setItems] = useState([]);
    const [selectedItemIds, setSelectedItemIds] = useState(() => {
        try {
            const savedItemIds = sessionStorage.getItem('gifthubSelectedItemIds');
            return savedItemIds ? JSON.parse(savedItemIds) : [];
        } catch (error) {
            console.error('Failed to parse gift hub selection state:', error);
            return [];
        }
    });
    const [modalShowState, setModalShowState] = useState(false);

    const selectedItems = useMemo(
        () => items.filter((item) => selectedItemIds.includes(item.giftHubItemId)),
        [items, selectedItemIds]
    );

    const totalPrice = useMemo(
        () => selectedItems.reduce(
            (accumulator, currentItem) => accumulator + (currentItem.itemPrice * currentItem.quantity),
            0
        ),
        [selectedItems]
    );

    const handleCheckboxChange = (giftHubItemId, isChecked) => {
        setSelectedItemIds((prev) => {
            if (isChecked) {
                return prev.includes(giftHubItemId) ? prev : [...prev, giftHubItemId];
            }
            return prev.filter((id) => id !== giftHubItemId);
        });
    };

    const handleDeleteItem = (giftHubItemId) => {
        setItems((prev) => prev.filter((item) => item.giftHubItemId !== giftHubItemId));
        setSelectedItemIds((prev) => prev.filter((id) => id !== giftHubItemId));
    };

    const handleQuantityChange = (giftHubItemId, newQuantity) => {
        setItems((prev) => prev.map((item) => (
            item.giftHubItemId === giftHubItemId
                ? { ...item, quantity: newQuantity }
                : item
        )));
    };

    useEffect(() => {
        sessionStorage.setItem('gifthubSelectedItemIds', JSON.stringify(selectedItemIds));
    }, [selectedItemIds]);

    useEffect(() => {
        const fetchData = async () => {
            try {
                const accessToken = localStorage.getItem('accessToken');
                if (!accessToken) {
                    setModalShowState(true);
                    return;
                }

                const response = await axios({
                    method: 'GET',
                    url: `${process.env.REACT_APP_FUNDINGBOOST}/gifthub`,
                    responseType: 'json',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${accessToken}`,
                    },
                });
                if (response.data.success) {
                    setItems(response.data.data);
                    const availableItemIds = new Set(response.data.data.map((item) => item.giftHubItemId));
                    setSelectedItemIds((prev) => prev.filter((id) => availableItemIds.has(id)));
                } else {
                    console.error('Failed to fetch data');
                }
            } catch (error) {
                console.error('There was an error fetching the post data:', error);
            }
        };

        fetchData();
    }, []);

    return(
        <div className="gifthub-page-container">
            {modalShowState && <NonMemberModal message="로그인 후 펀딩부스트를 시작해보세요" />}
            {items.length > 0 ? (
                <div className="gifthub-item-pane-container">
                    {items.map((item) => (
                        <SingleGiftHubItem
                            key={item.giftHubItemId}
                            item={item}
                            isSelected={selectedItemIds.includes(item.giftHubItemId)}
                            onCheckboxChange={handleCheckboxChange}
                            onDelete={handleDeleteItem}
                            onQuantityChange={handleQuantityChange}
                        />
                    ))}
                </div>
            ) : (
                <div className="gifthub-item-pane-container">
                    <GifthubNonItem/>
                </div>
            )}
            <div className="gifthub-result-pane">
                <GifthubResult totalPrice={totalPrice} selectedItems={selectedItems} />
            </div>
        </div>
    );
};

export default GifthubPane;
