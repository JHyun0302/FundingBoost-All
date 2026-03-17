import React, { useEffect, useMemo, useState } from "react";
import "./deliveryList.scss";
import DeliveryAddress from "../deliveryAddress/deliveryAddress";
import { formatPhoneNumber } from "../../../utils/formatPhoneNumber";

const ITEMS_PER_PAGE = 10;
const INITIAL_FORM = {
    customerName: "",
    phoneNumber: "",
    postalCode: "",
    address: "",
    deliveryMemo: "",
};

export default function DeliveryAddressList({ deliveryData, onCreateDelivery, onDeleteDelivery }) {
    const myPageDeliveryDtoList = deliveryData?.myPageDeliveryDtoList ?? [];
    const [currentPage, setCurrentPage] = useState(1);
    const [formData, setFormData] = useState(INITIAL_FORM);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const [showForm, setShowForm] = useState(false);

    const totalPages = Math.max(1, Math.ceil(myPageDeliveryDtoList.length / ITEMS_PER_PAGE));
    const pagedDeliveryDtoList = useMemo(() => {
        const startIndex = (currentPage - 1) * ITEMS_PER_PAGE;
        return myPageDeliveryDtoList.slice(startIndex, startIndex + ITEMS_PER_PAGE);
    }, [currentPage, myPageDeliveryDtoList]);

    useEffect(() => {
        setCurrentPage(1);
    }, [myPageDeliveryDtoList.length]);

    const handleInputChange = (event) => {
        const { name, value } = event.target;
        setFormData((prev) => ({
            ...prev,
            [name]: name === "phoneNumber" ? formatPhoneNumber(value) : value,
        }));
    };

    const handleCreate = async () => {
        if (!formData.customerName.trim() || !formData.phoneNumber.trim() || !formData.address.trim()) {
            alert("받는 사람, 연락처, 주소는 필수입니다.");
            return;
        }

        try {
            setIsSubmitting(true);
            await onCreateDelivery({
                customerName: formData.customerName.trim(),
                phoneNumber: formData.phoneNumber.trim(),
                postalCode: formData.postalCode.trim(),
                address: formData.address.trim(),
                deliveryMemo: formData.deliveryMemo.trim(),
            });
            setFormData(INITIAL_FORM);
            setShowForm(false);
        } catch (error) {
            console.error("배송지 추가 실패:", error);
            alert(error?.response?.data?.error?.message || "배송지 추가에 실패했습니다.");
        } finally {
            setIsSubmitting(false);
        }
    };

    const handleDelete = async (deliveryId) => {
        try {
            await onDeleteDelivery(deliveryId);
        } catch (error) {
            console.error("배송지 삭제 실패:", error);
            alert(error?.response?.data?.error?.message || "배송지 삭제에 실패했습니다.");
        }
    };

    return (
        <div className="deliveryList">
            <div className="deliveryAddressList-container">
                <div className="deliveryList-box">
                    <div className="delivery-address-list">
                        <div className="delivery-address-list-head">
                            <div className="delivery-address-list-delivery-management">배송지 관리</div>
                            <button
                                type="button"
                                className="delivery-address-list-add-delivery"
                                onClick={() => setShowForm((prev) => !prev)}
                            >
                                {showForm ? "입력 닫기" : "배송지 추가"}
                            </button>
                        </div>
                        {showForm && (
                            <div className="delivery-create-card">
                                <div className="delivery-create-grid">
                                    <input
                                        name="customerName"
                                        value={formData.customerName}
                                        onChange={handleInputChange}
                                        placeholder="받는 사람"
                                    />
                                    <input
                                        name="phoneNumber"
                                        value={formData.phoneNumber}
                                        onChange={handleInputChange}
                                        placeholder="연락처"
                                    />
                                    <input
                                        name="postalCode"
                                        value={formData.postalCode}
                                        onChange={handleInputChange}
                                        placeholder="우편번호"
                                    />
                                    <input
                                        name="address"
                                        value={formData.address}
                                        onChange={handleInputChange}
                                        placeholder="배송지 주소"
                                        className="delivery-create-address"
                                    />
                                    <textarea
                                        name="deliveryMemo"
                                        value={formData.deliveryMemo}
                                        onChange={handleInputChange}
                                        placeholder="배송 메모 (선택)"
                                    />
                                </div>
                                <div className="delivery-create-actions">
                                    <button
                                        type="button"
                                        className="delivery-create-submit"
                                        onClick={handleCreate}
                                        disabled={isSubmitting}
                                    >
                                        {isSubmitting ? "추가 중..." : "저장"}
                                    </button>
                                </div>
                            </div>
                        )}
                        <div className="delivery-address-list-line"/>

                        {pagedDeliveryDtoList.map((deliveryAddressData, index) => (
                        <div  key={index} className="delivery-address-list-addresses">
                            <DeliveryAddress
                                deliveryData={deliveryAddressData}
                                onDelete={handleDelete}
                            />
                        </div>
                        ))}
                        {myPageDeliveryDtoList.length === 0 && (
                            <div className="delivery-empty-state">등록된 배송지가 없습니다.</div>
                        )}
                        {myPageDeliveryDtoList.length > ITEMS_PER_PAGE && (
                            <div className="deliveryPagination">
                                <button
                                    type="button"
                                    className="deliveryPageButton"
                                    onClick={() => setCurrentPage((prev) => Math.max(prev - 1, 1))}
                                    disabled={currentPage === 1}
                                >
                                    이전
                                </button>
                                <div className="deliveryPageNumbers">
                                    {Array.from({ length: totalPages }, (_, index) => {
                                        const pageNumber = index + 1;
                                        return (
                                            <button
                                                type="button"
                                                key={pageNumber}
                                                className={`deliveryPageButton ${currentPage === pageNumber ? 'active' : ''}`}
                                                onClick={() => setCurrentPage(pageNumber)}
                                            >
                                                {pageNumber}
                                            </button>
                                        );
                                    })}
                                </div>
                                <button
                                    type="button"
                                    className="deliveryPageButton"
                                    onClick={() => setCurrentPage((prev) => Math.min(prev + 1, totalPages))}
                                    disabled={currentPage === totalPages}
                                >
                                    다음
                                </button>
                            </div>
                        )}

                    </div>
                </div>
            </div>

        </div>

    );
};
