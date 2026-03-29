package kcs.funding.payment.proxy;

import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Locale;
import java.util.Set;
import kcs.funding.payment.api.PaymentProxyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class PaymentProxyService {

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

    private final HttpClient httpClient;
    private final String baseUrl;
    private final long readTimeoutMs;

    public PaymentProxyService(
            @Value("${app.payment.proxy.base-url:http://fundingboost-server:8080}") String baseUrl,
            @Value("${app.payment.proxy.connect-timeout-ms:3000}") long connectTimeoutMs,
            @Value("${app.payment.proxy.read-timeout-ms:10000}") long readTimeoutMs
    ) {
        this.baseUrl = trimTrailingSlash(baseUrl);
        this.readTimeoutMs = readTimeoutMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
    }

    public ResponseEntity<byte[]> forward(HttpServletRequest request, HttpHeaders incomingHeaders, byte[] body) {
        return forwardTo(
                request.getMethod(),
                request.getRequestURI(),
                request.getQueryString(),
                incomingHeaders,
                body,
                "proxy"
        );
    }

    public ResponseEntity<byte[]> forwardToInternalCommand(
            HttpServletRequest request,
            HttpHeaders incomingHeaders,
            byte[] body
    ) {
        String internalPath = request.getRequestURI()
                .replaceFirst("^/api/v1/pay", "/internal/payment/commands");
        return forwardTo(
                request.getMethod(),
                internalPath,
                request.getQueryString(),
                incomingHeaders,
                body,
                "command-proxy"
        );
    }

    public ResponseEntity<byte[]> forwardTo(
            String method,
            String path,
            String queryString,
            HttpHeaders incomingHeaders,
            byte[] body,
            String serviceMode
    ) {
        HttpRequest.Builder outboundRequest = HttpRequest.newBuilder()
                .uri(resolveTargetUri(path, queryString))
                .timeout(Duration.ofMillis(readTimeoutMs))
                .method(method, bodyPublisher(body));

        copyRequestHeaders(incomingHeaders, outboundRequest);
        outboundRequest.header("X-Forwarded-By", "fundingboost-payment");

        try {
            HttpResponse<byte[]> response = httpClient.send(
                    outboundRequest.build(),
                    HttpResponse.BodyHandlers.ofByteArray()
            );

            HttpHeaders responseHeaders = new HttpHeaders();
            response.headers().map().forEach((name, values) -> {
                if (RESPONSE_HEADERS_TO_SKIP.contains(name.toLowerCase(Locale.ROOT))) {
                    return;
                }
                responseHeaders.addAll(name, values);
            });
            responseHeaders.set("X-Payment-Service-Mode", serviceMode);

            return new ResponseEntity<>(
                    response.body(),
                    responseHeaders,
                    HttpStatusCode.valueOf(response.statusCode())
            );
        } catch (IOException exception) {
            throw new PaymentProxyException("payment proxy I/O failure", exception);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new PaymentProxyException("payment proxy interrupted", exception);
        }
    }

    private URI resolveTargetUri(String path, String queryString) {
        StringBuilder uriBuilder = new StringBuilder(baseUrl).append(path);
        if (queryString != null && !queryString.isBlank()) {
            uriBuilder.append('?').append(queryString);
        }
        return URI.create(uriBuilder.toString());
    }

    private void copyRequestHeaders(HttpHeaders incomingHeaders, HttpRequest.Builder outboundRequest) {
        incomingHeaders.forEach((name, values) -> {
            if (REQUEST_HEADERS_TO_SKIP.contains(name.toLowerCase(Locale.ROOT))) {
                return;
            }
            for (String value : values) {
                outboundRequest.header(name, value);
            }
        });
    }

    private HttpRequest.BodyPublisher bodyPublisher(byte[] body) {
        if (body == null || body.length == 0) {
            return HttpRequest.BodyPublishers.noBody();
        }
        return HttpRequest.BodyPublishers.ofByteArray(body);
    }

    private String trimTrailingSlash(String value) {
        String normalized = value == null ? "" : value.trim();
        if (normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }
}
