package kcs.funding.fundingboost.domain.dto.request.giftHub;

public record AddGiftHubDto(int quantity, String optionName) {
    public AddGiftHubDto(int quantity) {
        this(quantity, null);
    }
}
