package kcs.funding.payment.nativeflow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import kcs.funding.payment.api.PaymentProxyException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class NativePaymentBridgeClient {

    private static final Set<String> REQUEST_HEADERS_TO_SKIP = Set.of(
            "host",
            "connection",
            "content-length",
            "transfer-encoding"
    );
    private static final Set<String> RESPONSE_HEADERS_TO_SKIP = Set.of(
            "connection",
            "content-length",
            "transfer-encoding"
    );

    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(3000))
            .build();

    @Value("${app.payment.proxy.base-url:http://fundingboost-server:8080}")
    private String baseUrl;

    @Value("${app.payment.proxy.read-timeout-ms:10000}")
    private long readTimeoutMs;

    public NativeOrderPreparationDto prepareOrder(HttpHeaders incomingHeaders, byte[] body) {
        return postForData("/internal/payment/native/order/prepare", incomingHeaders, body, NativeOrderPreparationDto.class);
    }

    public NativeOrderPreparationDto prepareOrderNow(HttpHeaders incomingHeaders, byte[] body) {
        return postForData("/internal/payment/native/order/now/prepare", incomingHeaders, body, NativeOrderPreparationDto.class);
    }

    public NativeOrderPreparationDto prepareFunding(Long fundingItemId, HttpHeaders incomingHeaders, byte[] body) {
        return postForData(
                "/internal/payment/native/funding/" + fundingItemId + "/prepare",
                incomingHeaders,
                body,
                NativeOrderPreparationDto.class
        );
    }

    public NativeOrderFinalizeResultDto finalizeOrder(
            HttpHeaders incomingHeaders,
            JsonNode payload,
            String paymentIntentKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount,
            String pgProvider,
            String pgTransactionId
    ) {
        JsonNode requestBody = objectMapper.createObjectNode()
                .set("payload", payload);
        ((com.fasterxml.jackson.databind.node.ObjectNode) requestBody)
                .put("paymentIntentKey", paymentIntentKey)
                .put("totalAmount", totalAmount)
                .put("pointAmount", pointAmount)
                .put("pgAmount", pgAmount)
                .put("fundingSupportedAmount", fundingSupportedAmount)
                .put("pgProvider", pgProvider == null ? "" : pgProvider)
                .put("pgTransactionId", pgTransactionId == null ? "" : pgTransactionId);
        return postForData("/internal/payment/native/order/finalize", incomingHeaders, writeJson(requestBody), NativeOrderFinalizeResultDto.class);
    }

    public NativeOrderFinalizeResultDto finalizeOrderNow(
            HttpHeaders incomingHeaders,
            JsonNode payload,
            String paymentIntentKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount,
            String pgProvider,
            String pgTransactionId
    ) {
        JsonNode requestBody = objectMapper.createObjectNode()
                .set("payload", payload);
        ((com.fasterxml.jackson.databind.node.ObjectNode) requestBody)
                .put("paymentIntentKey", paymentIntentKey)
                .put("totalAmount", totalAmount)
                .put("pointAmount", pointAmount)
                .put("pgAmount", pgAmount)
                .put("fundingSupportedAmount", fundingSupportedAmount)
                .put("pgProvider", pgProvider == null ? "" : pgProvider)
                .put("pgTransactionId", pgTransactionId == null ? "" : pgTransactionId);
        return postForData("/internal/payment/native/order/now/finalize", incomingHeaders, writeJson(requestBody), NativeOrderFinalizeResultDto.class);
    }

    public NativeOrderFinalizeResultDto finalizeFunding(
            Long fundingItemId,
            HttpHeaders incomingHeaders,
            JsonNode payload,
            String paymentIntentKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount,
            String pgProvider,
            String pgTransactionId
    ) {
        JsonNode requestBody = objectMapper.createObjectNode()
                .set("payload", payload);
        ((com.fasterxml.jackson.databind.node.ObjectNode) requestBody)
                .put("paymentIntentKey", paymentIntentKey)
                .put("totalAmount", totalAmount)
                .put("pointAmount", pointAmount)
                .put("pgAmount", pgAmount)
                .put("fundingSupportedAmount", fundingSupportedAmount)
                .put("pgProvider", pgProvider == null ? "" : pgProvider)
                .put("pgTransactionId", pgTransactionId == null ? "" : pgTransactionId);
        return postForData(
                "/internal/payment/native/funding/" + fundingItemId + "/finalize",
                incomingHeaders,
                writeJson(requestBody),
                NativeOrderFinalizeResultDto.class
        );
    }

    private <T> T postForData(String path, HttpHeaders incomingHeaders, byte[] body, Class<T> responseType) {
        ResponseEntity<byte[]> response = exchange(path, incomingHeaders, body);
        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new BackBridgeResponseException(response.getStatusCode(), response.getHeaders(), response.getBody());
        }
        try {
            JsonNode root = objectMapper.readTree(response.getBody());
            if (!root.path("success").asBoolean(false)) {
                throw new BackBridgeResponseException(response.getStatusCode(), response.getHeaders(), response.getBody());
            }
            return objectMapper.treeToValue(root.path("data"), responseType);
        } catch (BackBridgeResponseException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new PaymentProxyException("failed to parse back bridge response", exception);
        }
    }

    private ResponseEntity<byte[]> exchange(String path, HttpHeaders incomingHeaders, byte[] body) {
        HttpRequest.Builder outboundRequest = HttpRequest.newBuilder()
                .uri(URI.create(trimTrailingSlash(baseUrl) + path))
                .timeout(Duration.ofMillis(readTimeoutMs))
                .POST(body == null || body.length == 0
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofByteArray(body));

        incomingHeaders.forEach((name, values) -> {
            if (REQUEST_HEADERS_TO_SKIP.contains(name.toLowerCase(Locale.ROOT))) {
                return;
            }
            for (String value : values) {
                outboundRequest.header(name, value);
            }
        });
        outboundRequest.header("Content-Type", "application/json");
        outboundRequest.header("X-Forwarded-By", "fundingboost-payment-native");

        try {
            HttpResponse<byte[]> response = httpClient.send(outboundRequest.build(), HttpResponse.BodyHandlers.ofByteArray());
            HttpHeaders responseHeaders = new HttpHeaders();
            response.headers().map().forEach((name, values) -> {
                if (RESPONSE_HEADERS_TO_SKIP.contains(name.toLowerCase(Locale.ROOT))) {
                    return;
                }
                responseHeaders.addAll(name, values);
            });
            return new ResponseEntity<>(response.body(), responseHeaders, HttpStatusCode.valueOf(response.statusCode()));
        } catch (IOException exception) {
            throw new PaymentProxyException("payment native bridge I/O failure", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaymentProxyException("payment native bridge interrupted", exception);
        }
    }

    private byte[] writeJson(JsonNode jsonNode) {
        try {
            return objectMapper.writeValueAsBytes(jsonNode);
        } catch (IOException exception) {
            throw new PaymentProxyException("failed to serialize native payment bridge request", exception);
        }
    }

    private String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
