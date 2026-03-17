package kcs.funding.fundingboost.domain.dto.request.myPage.deliveryManage;

public record CreateDeliveryRequestDto(
        String customerName,
        String phoneNumber,
        String postalCode,
        String address,
        String deliveryMemo
) {
}
