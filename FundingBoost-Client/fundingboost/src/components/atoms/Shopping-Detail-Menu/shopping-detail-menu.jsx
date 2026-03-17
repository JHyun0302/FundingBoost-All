import React, { useEffect, useMemo, useState } from "react";
import "./shopping-detail-menu.scss";
import Form from "react-bootstrap/Form";
import { useNavigate } from "react-router-dom";
import share from "./../../../assets/share.svg";
import gifthub from "./../../../assets/gifthub.svg";
import WishBtn from "../button/wishBtn/wishBtn";
import FundingNowBtn from "../button/fundingNowBtn/fundingNowBtn";
import PurchaseBtn from "../button/purchaseBtn/purchaseBtn";
import GifthubModal from "../gifthubModal/gifthubModal";
import NonMemberModal from "../shoppingDetail-nonMemberModal/shoppingDetail-nonMemberModal";

const priceFormatter = new Intl.NumberFormat("ko-KR");

export default function ShoppingDetailMenu({
    itemId,
    itemName,
    itemPrice,
    options,
    itemThumbnailImageUrl,
    bookmark
}) {
    const navigate = useNavigate();
    const [quantity, setQuantity] = useState(1);
    const [selectOption, setSelectOptions] = useState("");
    const [showModal, setShowModal] = useState(false);
    const [showNonMemberModal, setShowNonMemberModal] = useState(false);

    const closeModal = () => {
        setShowModal(false);
    };

    const gifthubBtnModal = () => {
        setShowModal(false);
        navigate("/gifthub");
    };

    const openNonMemberModal = () => {
        setShowNonMemberModal(true);
    };

    const closeNonMemberModal = () => {
        setShowNonMemberModal(false);
    };

    const parsedOptions = useMemo(() => {
        if (!Array.isArray(options) || options.length === 0) {
            return ["기본 옵션"];
        }

        const normalizedOptions = options
            .map((value) => (value || "").trim())
            .filter((value) => value.length > 0)
            .filter((value) => !value.includes("상품 옵션을 선택"));

        if (normalizedOptions.length === 0) {
            return ["기본 옵션"];
        }

        return Array.from(new Set(normalizedOptions));
    }, [options]);

    const requiresExplicitSelection = parsedOptions.length > 1;

    useEffect(() => {
        if (requiresExplicitSelection) {
            setSelectOptions("");
            return;
        }

        setSelectOptions(parsedOptions[0]);
    }, [parsedOptions, requiresExplicitSelection]);

    const optionChange = (e) => {
        setSelectOptions(e.target.value);
    };

    const decreaseQuantity = () => {
        if (quantity > 1) {
            setQuantity(quantity - 1);
        }
    };

    const increaseQuantity = () => {
        setQuantity(quantity + 1);
    };

    const handleShareClick = async () => {
        const shareUrl = window.location.href;

        try {
            if (navigator?.clipboard?.writeText) {
                await navigator.clipboard.writeText(shareUrl);
                alert("상품 링크가 복사되었습니다.");
                return;
            }
        } catch (error) {
            // Clipboard API 실패 시 fallback 수행
        }

        const temporaryInput = document.createElement("input");
        temporaryInput.value = shareUrl;
        document.body.appendChild(temporaryInput);
        temporaryInput.select();
        document.execCommand("copy");
        document.body.removeChild(temporaryInput);
        alert("상품 링크가 복사되었습니다.");
    };

    const handleGiftHubClick = async () => {
        const accessToken = localStorage.getItem("accessToken");

        if (!accessToken) {
            openNonMemberModal();
            return;
        }

        if (requiresExplicitSelection && (!selectOption || selectOption === "상품 옵션을 선택해주세요.")) {
            alert("상품 옵션을 선택해주세요");
            return;
        }

        try {
            const selectedOptionName = (selectOption && selectOption.trim().length > 0)
                ? selectOption
                : (parsedOptions[0] || "기본 옵션");

            const response = await fetch(`${process.env.REACT_APP_FUNDINGBOOST}/gifthub/${itemId}`, {
                method: "POST",
                headers: {
                    "Content-Type": "application/json",
                    Authorization: `Bearer ${accessToken}`
                },
                body: JSON.stringify({
                    quantity,
                    optionName: selectedOptionName
                })
            });

            if (!response.ok) {
                throw new Error(`gift hub request failed: ${response.status}`);
            }

            setShowModal(true);
        } catch (error) {
            console.error("Error:", error);
            alert("GiftHub 담기에 실패했습니다. 잠시 후 다시 시도해주세요.");
        }
    };

    const totalPrice = (itemPrice || 0) * quantity;

    return (
        <div className="shopping-detail-action-card">
            <NonMemberModal
                message="로그인이 필요한 서비스입니다."
                onClose={closeNonMemberModal}
                show={showNonMemberModal}
            />
            <GifthubModal
                itemName={itemName}
                show={showModal}
                onClose={closeModal}
                onGiftHub={gifthubBtnModal}
                message="Gifthub에 상품이 담겼습니다."
            />

            <div className="shopping-detail-quantity-row">
                <span className="shopping-detail-label">수량</span>
                <div className="shopping-detail-stepper" aria-label="수량 조절">
                    <button type="button" className="quantity-button" onClick={decreaseQuantity}>-</button>
                    <span className="quantity-value">{quantity}</span>
                    <button type="button" className="quantity-button" onClick={increaseQuantity}>+</button>
                </div>
                <strong className="shopping-detail-total-price">{priceFormatter.format(totalPrice)}원</strong>
            </div>

            <div className="shopping-detail-option-row">
                <label htmlFor="shopping-option-select" className="shopping-detail-label">옵션 선택</label>
                <Form.Select
                    id="shopping-option-select"
                    aria-label="상품 옵션 선택"
                    onChange={optionChange}
                    value={selectOption}
                >
                    {requiresExplicitSelection && (
                        <option value="">상품 옵션을 선택해주세요.</option>
                    )}
                    {parsedOptions.map((optionValue) => (
                        <option key={optionValue} value={optionValue}>
                            {optionValue}
                        </option>
                    ))}
                </Form.Select>
            </div>

            <div className="shopping-detail-action-row">
                <button
                    type="button"
                    className="shopping-detail-icon-button"
                    onClick={handleShareClick}
                    aria-label="상품 링크 공유"
                >
                    <img className="share-icon" alt="shareIcon" src={share} />
                </button>
                <WishBtn itemId={itemId} bookmark={bookmark} onNonMemberModalOpen={openNonMemberModal} />
            </div>

            <div className="shopping-detail-purchase-row">
                <PurchaseBtn
                    itemId={itemId}
                    itemThumbnailImageUrl={itemThumbnailImageUrl}
                    selectOption={selectOption}
                    itemPrice={itemPrice}
                    itemName={itemName}
                    quantity={quantity}
                    onNonMemberModalOpen={openNonMemberModal}
                />
                <FundingNowBtn
                    itemId={itemId}
                    itemThumbnailImageUrl={itemThumbnailImageUrl}
                    selectOption={selectOption}
                    itemPrice={itemPrice}
                    itemName={itemName}
                    quantity={quantity}
                    onNonMemberModalOpen={openNonMemberModal}
                />
            </div>

            <button type="button" className="shopping-detail-gifthub-button" onClick={handleGiftHubClick}>
                <img className="gifthub-icon" alt="gifthubImg" src={gifthub} />
                <span>GiftHub 담기</span>
            </button>

            <ul className="shopping-detail-help-text">
                <li>로그인 후 구매/펀딩/위시 기능을 사용할 수 있습니다.</li>
                <li>펀딩은 아이템당 최대 5개 수량까지 등록 가능합니다.</li>
            </ul>
        </div>
    );
}
