package kcs.funding.fundingboost.domain.dto.request.pay.myPay;

public record ItemPayNowDto(
        Long itemId,
        int quantity,
        Long deliveryId,
        int usingPoint,
        String optionName
) {
    public ItemPayNowDto(Long itemId, int quantity, Long deliveryId, int usingPoint) {
        this(itemId, quantity, deliveryId, usingPoint, null);
    }
}
