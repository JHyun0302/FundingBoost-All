package kcs.funding.payment.common.dto;

public record CommonSuccessDto(boolean isSuccess) {

    public static CommonSuccessDto success() {
        return new CommonSuccessDto(true);
    }
}
