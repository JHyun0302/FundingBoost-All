import React, { useState,useEffect } from 'react';
import './fundingRegistItem.scss';
import NonItemImg from "../../../../assets/nonItemImg.svg";
import { toImageProxyUrl } from "../../../../utils/imageProxyUrl";

const FundingRegistItem = ({ selectedItems, onItemOrderChange, onItemDelete }) => {
    const [orderedItems, setOrderedItems] = useState(selectedItems);

    useEffect(() => {
        setOrderedItems(selectedItems);
    }, [selectedItems]);
    // console.log(orderedItems);

    //상품 순서 지정
    const itemOrderCheck = (index) => {
        const updatedItems = [...orderedItems];
        const selectedItem = updatedItems[index];

        const isSelected = !!selectedItem.order;

        //선택 순서 조정
        if (isSelected) {
            const selectedOrder = selectedItem.order;
            selectedItem.order = null;

            //선택 해제 시 순서 재조정
            updatedItems.forEach(item => {
                if (item.order && item.order > selectedOrder) {
                    item.order--;
                }
            });
        } else {
            const lastOrder = Math.max(...updatedItems.map(item => item.order || 0));
            selectedItem.order = lastOrder + 1;
        }

        //순서대로 정렬
        updatedItems[index] = selectedItem;
        updatedItems.sort((a, b) => (a.order || Infinity) - (b.order || Infinity));

        setOrderedItems(updatedItems);
        onItemOrderChange(updatedItems);
    };

    return (
        <div className="FundingRegistItem">
            <div className="fundingRegist-text">
                * 모든 상품의 순서를 지정해주세요.
            </div>
            {orderedItems.map((item, index) => (
                <div key={item.id || `${item.itemId}-${index}`}>
                    <div
                        className="itemContainer"
                        role="button"
                        tabIndex={0}
                        onClick={() => itemOrderCheck(index)}
                        onKeyDown={(event) => {
                            if (event.key === "Enter" || event.key === " ") {
                                event.preventDefault();
                                itemOrderCheck(index);
                            }
                        }}
                    >
                        <input
                            type="checkbox"
                            id={`checkbox-${index}`}
                            checked={!!item.order}
                            onClick={(event) => event.stopPropagation()}
                            onChange={() => itemOrderCheck(index)}
                        />
                        <label
                            className="checkbox-label"
                            htmlFor={`checkbox-${index}`}
                            data-order={item.order || ''}
                            onClick={(event) => event.stopPropagation()}
                        ></label>
                        <img src={toImageProxyUrl(item.itemImageUrl || NonItemImg)} alt={item.itemName} className="item-img" />
                        <div className="itemDetail">
                            <div className="title">{item.itemName}</div>
                            <div className="optionDetail">
                                <div className="optionGroup">
                                    <div className="option">옵션</div>
                                </div>
                                <div className="optionName">{item.optionName}</div>
                            </div>
                            <div className="price">{item.itemPrice.toLocaleString()} 원</div>
                        </div>
                        <button
                            type="button"
                            className="itemDeleteBtn"
                            onClick={(event) => {
                                event.stopPropagation();
                                onItemDelete?.(item.id);
                            }}
                        >
                            삭제
                        </button>
                    </div>
                </div>
            ))}
        </div>
    );
};

export default FundingRegistItem;
