import React, { useEffect, useMemo, useRef, useState } from 'react';
import './mypage-myfunding-itemlist.scss';
import MyFundingSingleItem from '../../atoms/mypage-myfunding-singleitem/mypage-myfunding-singleitem';
import Carousel from 'react-bootstrap/Carousel';

export default function MyPageFundingItemPane({ apiData, myPageFundingItemDtoList, isFundingClosed, setIsFundingClosed }) {
    const [index, setIndex] = useState(0);
    const [itemsPerSlide, setItemsPerSlide] = useState(3);
    const groupRef = useRef(null);

    useEffect(() => {
        const updateItemsPerSlide = () => {
            const containerWidth = groupRef.current?.clientWidth || window.innerWidth;
            if (containerWidth < 680) {
                setItemsPerSlide(1);
                return;
            }
            if (containerWidth < 1040) {
                setItemsPerSlide(2);
                return;
            }
            setItemsPerSlide(3);
        };

        updateItemsPerSlide();

        const resizeObserver = new ResizeObserver(updateItemsPerSlide);
        if (groupRef.current) {
            resizeObserver.observe(groupRef.current);
        }
        window.addEventListener('resize', updateItemsPerSlide);

        return () => {
            resizeObserver.disconnect();
            window.removeEventListener('resize', updateItemsPerSlide);
        };
    }, []);

    const slideItems = useMemo(() => {
        const chunks = [];
        for (let i = 0; i < myPageFundingItemDtoList.length; i += itemsPerSlide) {
            chunks.push(myPageFundingItemDtoList.slice(i, i + itemsPerSlide));
        }
        return chunks;
    }, [itemsPerSlide, myPageFundingItemDtoList]);

    const handleSelect = (selectedIndex) => {
        setIndex(selectedIndex);
    };

    useEffect(() => {
        if (index >= slideItems.length) {
            setIndex(0);
        }
    }, [index, slideItems.length]);

    return (
        <div className="myPageFundingItemdGroupBox" ref={groupRef}>
            <div className="MyPageFundingItemGroupView">
                <Carousel
                    className="mypage-funding-carousel"
                    activeIndex={index}
                    onSelect={handleSelect}
                    indicators={false}
                    controls={slideItems.length > 1}
                >
                    {slideItems.map((items, slideIndex) => (
                        <Carousel.Item key={`mypage-funding-slide-${slideIndex}`}>
                            <div className="mypage-myfunding-item-view">
                                {items.map((item, itemIndex) => (
                                    <MyFundingSingleItem
                                        key={`${item.itemId}-${slideIndex}-${itemIndex}`}
                                        item={item}
                                        isFundingClosed={isFundingClosed}
                                        setIsFundingClosed={setIsFundingClosed}
                                        apiData={apiData}
                                        myPageFundingItemDtoList={myPageFundingItemDtoList}
                                    />
                                ))}
                            </div>
                        </Carousel.Item>
                    ))}
                </Carousel>
            </div>
        </div>
    );
};
