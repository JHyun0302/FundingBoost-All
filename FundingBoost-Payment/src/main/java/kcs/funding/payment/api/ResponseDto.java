package kcs.funding.payment.api;

public record ResponseDto<T>(
        boolean success,
        T data,
        ErrorDto error
) {

    public static <T> ResponseDto<T> ok(T data) {
        return new ResponseDto<>(true, data, null);
    }

    public static <T> ResponseDto<T> fail(int code, String message) {
        return new ResponseDto<>(false, null, new ErrorDto(code, message));
    }

    public record ErrorDto(
            int code,
            String message
    ) {
    }
}
