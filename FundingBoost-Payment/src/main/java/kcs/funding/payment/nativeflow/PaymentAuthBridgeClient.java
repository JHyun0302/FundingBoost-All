package kcs.funding.payment.nativeflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import kcs.funding.payment.api.PaymentProxyException;
import kcs.funding.payment.proxy.PaymentProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class PaymentAuthBridgeClient {

    private final PaymentProxyService paymentProxyService;
    private final ObjectMapper objectMapper;

    public Long resolveMemberId(HttpHeaders incomingHeaders) {
        ResponseEntity<byte[]> response = paymentProxyService.forwardTo(
                "GET",
                "/internal/payment/auth/me",
                null,
                incomingHeaders,
                null,
                "auth-bridge"
        );
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new BackBridgeResponseException(response.getStatusCode(), response.getHeaders(), response.getBody());
        }
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.path("success").asBoolean(false)) {
                throw new BackBridgeResponseException(response.getStatusCode(), response.getHeaders(), response.getBody());
            }
            JsonNode memberIdNode = root.path("data").path("memberId");
            if (!memberIdNode.canConvertToLong()) {
                throw new PaymentProxyException("failed to resolve authenticated member id");
            }
            return memberIdNode.asLong();
        } catch (BackBridgeResponseException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PaymentProxyException("failed to parse payment auth bridge response", exception);
        }
    }
}
