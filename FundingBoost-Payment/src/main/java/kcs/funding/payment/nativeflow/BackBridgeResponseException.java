package kcs.funding.payment.nativeflow;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;

public class BackBridgeResponseException extends RuntimeException {

    private final HttpStatusCode statusCode;
    private final HttpHeaders headers;
    private final byte[] body;

    public BackBridgeResponseException(HttpStatusCode statusCode, HttpHeaders headers, byte[] body) {
        super("back bridge call failed");
        this.statusCode = statusCode;
        this.headers = headers;
        this.body = body;
    }

    public HttpStatusCode statusCode() {
        return statusCode;
    }

    public HttpHeaders headers() {
        return headers;
    }

    public byte[] body() {
        return body;
    }
}
