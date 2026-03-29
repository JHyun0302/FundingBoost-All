package kcs.funding.payment.api;

import jakarta.servlet.http.HttpServletRequest;
import kcs.funding.payment.nativeflow.NativeFriendPaymentService;
import kcs.funding.payment.nativeflow.NativeFundingPaymentService;
import kcs.funding.payment.nativeflow.NativeOrderPaymentService;
import kcs.funding.payment.proxy.PaymentProxyService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class PaymentWriteController {

    private final PaymentProxyService paymentProxyService;
    private final NativeOrderPaymentService nativeOrderPaymentService;
    private final NativeFundingPaymentService nativeFundingPaymentService;
    private final NativeFriendPaymentService nativeFriendPaymentService;

    @Value("${app.payment.native.order-flows-enabled:true}")
    private boolean nativeOrderFlowsEnabled;

    @Value("${app.payment.native.funding-flows-enabled:true}")
    private boolean nativeFundingFlowsEnabled;

    @Value("${app.payment.native.friend-flows-enabled:true}")
    private boolean nativeFriendFlowsEnabled;

    @RequestMapping(
            path = {"/api/v1/pay", "/api/v1/pay/**"},
            method = {RequestMethod.POST}
    )
    public ResponseEntity<byte[]> handlePaymentWrite(
            HttpServletRequest request,
            @RequestHeader HttpHeaders headers,
            @RequestBody(required = false) byte[] body
    ) {
        if (nativeOrderPaymentService.supports(request, nativeOrderFlowsEnabled)) {
            return nativeOrderPaymentService.handle(request, headers, body);
        }
        if (nativeFundingPaymentService.supports(request, nativeFundingFlowsEnabled)) {
            return nativeFundingPaymentService.handle(request, headers, body);
        }
        if (nativeFriendPaymentService.supports(request, nativeFriendFlowsEnabled)) {
            return nativeFriendPaymentService.handle(request, headers, body);
        }
        return paymentProxyService.forwardToInternalCommand(request, headers, body);
    }
}
