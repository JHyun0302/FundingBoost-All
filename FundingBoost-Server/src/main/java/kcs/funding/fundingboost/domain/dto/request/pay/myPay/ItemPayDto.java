package kcs.funding.fundingboost.domain.dto.request.pay.myPay;

public record ItemPayDto(Long itemId, Long giftHubId, int quantity, String optionName) {
    public ItemPayDto(Long itemId, Long giftHubId, int quantity) {
        this(itemId, giftHubId, quantity, null);
    }
}
