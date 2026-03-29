package kcs.funding.payment.api;

import jakarta.servlet.http.HttpServletRequest;
import java.time.OffsetDateTime;
import java.util.Map;
import kcs.funding.payment.proxy.PaymentProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping
public class PaymentSkeletonController {

    private final PaymentProxyService paymentProxyService;

    @GetMapping("/internal/payment/health")
    public ResponseDto<Map<String, Object>> health() {
        return ResponseDto.ok(Map.of(
                "service", "fundingboost-payment",
                "mode", "proxy",
                "status", "UP",
                "timestamp", OffsetDateTime.now().toString()
        ));
    }

    @RequestMapping(
            path = {"/api/v1/pay", "/api/v1/pay/**"},
            method = {RequestMethod.GET}
    )
    public ResponseEntity<byte[]> proxyPaymentApi(
            HttpServletRequest request,
            @RequestHeader HttpHeaders headers,
            @RequestBody(required = false) byte[] body
    ) {
        return paymentProxyService.forward(request, headers, body);
    }
}
