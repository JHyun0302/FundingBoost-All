package kcs.funding.fundingboost.domain.dto.response.myPage.deliveryManage;

import kcs.funding.fundingboost.domain.entity.Delivery;
import lombok.Builder;

@Builder
public record MyPageDeliveryDto(
        Long deliveryId,
        String customerName,
        String address,
        String phoneNumber,
        String postalCode,
        String deliveryMemo
) {
    public static MyPageDeliveryDto fromEntity(Delivery delivery) {
        return MyPageDeliveryDto.builder()
                .deliveryId(delivery.getDeliveryId())
                .customerName(delivery.getCustomerName())
                .address(delivery.getAddress())
                .phoneNumber(delivery.getPhoneNumber())
                .postalCode(delivery.getPostalCode())
                .deliveryMemo(delivery.getDeliveryMemo())
                .build();
    }
}
