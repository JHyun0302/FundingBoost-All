package kcs.funding.payment.nativeflow;

import static kcs.funding.payment.exception.ErrorCode.BAD_REQUEST_JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import kcs.funding.payment.api.ResponseDto;
import kcs.funding.payment.common.dto.CommonSuccessDto;
import kcs.funding.payment.exception.CommonException;
import kcs.funding.payment.friend.application.PaymentFriendWriteService;
import kcs.funding.payment.friend.dto.FriendPayBarcodeConsumeDto;
import kcs.funding.payment.friend.dto.FriendPayBarcodeIssueDto;
import kcs.funding.payment.friend.dto.FriendPayProcessDto;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class NativeFriendPaymentService {

    private final ObjectMapper objectMapper;
    private final PaymentAuthBridgeClient paymentAuthBridgeClient;
    private final PaymentFriendWriteService paymentFriendWriteService;

    @Value("${app.pay.barcode-verify-base-url:}")
    private String barcodeVerifyBaseUrl;

    public boolean supports(HttpServletRequest request, boolean enabled) {
        if (!enabled || !"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        return request.getRequestURI().matches("^/api/v1/pay/friends/\\d+($|/barcode-token$|/barcode-token/consume$)");
    }

    public ResponseEntity<byte[]> handle(HttpServletRequest request, HttpHeaders headers, byte[] body) {
        request.setAttribute("paymentServiceMode", "native-friend");
        try {
            Long memberId = paymentAuthBridgeClient.resolveMemberId(headers);
            Long fundingId = extractFundingId(request.getRequestURI());

            if (request.getRequestURI().endsWith("/barcode-token/consume")) {
                FriendPayBarcodeConsumeDto consumeDto = readBody(body, FriendPayBarcodeConsumeDto.class);
                CommonSuccessDto result = paymentFriendWriteService.fundWithBarcodeToken(memberId, fundingId, consumeDto);
                return successResponse(result);
            }
            if (request.getRequestURI().endsWith("/barcode-token")) {
                FriendPayProcessDto processDto = readBody(body, FriendPayProcessDto.class);
                FriendPayBarcodeIssueDto issued = paymentFriendWriteService.issueBarcodeToken(memberId, fundingId, processDto);
                FriendPayBarcodeIssueDto response = FriendPayBarcodeIssueDto.builder()
                        .token(issued.token())
                        .barcodeValue(issued.barcodeValue())
                        .verifyUrl(buildVerifyUrl(request, issued.token()))
                        .expiresAt(issued.expiresAt())
                        .usingPoint(issued.usingPoint())
                        .fundingPrice(issued.fundingPrice())
                        .build();
                return successResponse(response);
            }

            FriendPayProcessDto processDto = readBody(body, FriendPayProcessDto.class);
            CommonSuccessDto result = paymentFriendWriteService.fund(memberId, fundingId, processDto);
            return successResponse(result);
        } catch (BackBridgeResponseException exception) {
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.putAll(exception.headers());
            responseHeaders.set("X-Payment-Service-Mode", "native-friend");
            return new ResponseEntity<>(exception.body(), responseHeaders, exception.statusCode());
        }
    }

    private <T> T readBody(byte[] body, Class<T> targetType) {
        try {
            return objectMapper.readValue(body == null ? new byte[0] : body, targetType);
        } catch (IOException exception) {
            throw new CommonException(BAD_REQUEST_JSON);
        }
    }

    private Long extractFundingId(String path) {
        String[] segments = path.split("/");
        return Long.parseLong(segments[segments.length - (path.endsWith("/consume") ? 3 : path.endsWith("/barcode-token") ? 2 : 1)]);
    }

    private <T> ResponseEntity<byte[]> successResponse(T data) {
        try {
            byte[] responseBody = objectMapper.writeValueAsBytes(ResponseDto.ok(data));
            HttpHeaders responseHeaders = new HttpHeaders();
            responseHeaders.set("X-Payment-Service-Mode", "native-friend");
            return ResponseEntity.ok().headers(responseHeaders).body(responseBody);
        } catch (IOException exception) {
            throw new IllegalStateException("failed to serialize friend payment response", exception);
        }
    }

    private String buildVerifyUrl(HttpServletRequest request, String token) {
        if (barcodeVerifyBaseUrl != null && !barcodeVerifyBaseUrl.isBlank()) {
            String normalizedBaseUrl = barcodeVerifyBaseUrl.endsWith("/")
                    ? barcodeVerifyBaseUrl.substring(0, barcodeVerifyBaseUrl.length() - 1)
                    : barcodeVerifyBaseUrl;
            return normalizedBaseUrl + "/api/v1/pay/friends/barcode-token/" + token;
        }
        return ServletUriComponentsBuilder.fromRequestUri(request)
                .replacePath("/api/v1/pay/friends/barcode-token/{token}")
                .replaceQuery(null)
                .buildAndExpand(token)
                .toUriString();
    }
}
