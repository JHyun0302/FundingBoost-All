package kcs.funding.crawler.catalog.api;

public record ResponseDto<T>(
        boolean success,
        T data,
        ApiErrorDto error
) {

    public static <T> ResponseDto<T> ok(T data) {
        return new ResponseDto<>(true, data, null);
    }

    public static <T> ResponseDto<T> fail(int code, String message) {
        return new ResponseDto<>(false, null, new ApiErrorDto(code, message));
    }
}
