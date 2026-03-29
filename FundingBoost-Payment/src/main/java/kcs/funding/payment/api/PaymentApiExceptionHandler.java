package kcs.funding.payment.api;

import jakarta.servlet.http.HttpServletRequest;
import kcs.funding.payment.exception.CommonException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class PaymentApiExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ResponseDto<Void>> handleCommonException(CommonException exception, HttpServletRequest request) {
        return ResponseEntity.ok()
                .headers(serviceModeHeaders(request))
                .body(ResponseDto.fail(
                        exception.getErrorCode().getCode(),
                        exception.getErrorCode().getMessage()
                ));
    }

    @ExceptionHandler(PaymentProxyException.class)
    public ResponseEntity<ResponseDto<Void>> handlePaymentProxyException(PaymentProxyException exception, HttpServletRequest request) {
        log.warn("payment proxy failed: {}", exception.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .headers(serviceModeHeaders(request))
                .body(ResponseDto.fail(50200, "Payment proxy request failed."));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ResponseDto<Void>> handleUnhandledException(Exception exception, HttpServletRequest request) {
        log.error("payment api unhandled exception", exception);
        return ResponseEntity.ok()
                .headers(serviceModeHeaders(request))
                .body(ResponseDto.fail(50000, "서버 내부 에러입니다."));
    }

    private HttpHeaders serviceModeHeaders(HttpServletRequest request) {
        HttpHeaders headers = new HttpHeaders();
        if (request == null) {
            return headers;
        }
        Object serviceMode = request.getAttribute("paymentServiceMode");
        if (serviceMode instanceof String mode && !mode.isBlank()) {
            headers.set("X-Payment-Service-Mode", mode);
        }
        return headers;
    }
}
