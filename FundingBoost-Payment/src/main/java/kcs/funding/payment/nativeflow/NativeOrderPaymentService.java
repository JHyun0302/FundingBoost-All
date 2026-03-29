package kcs.funding.payment.nativeflow;

import static kcs.funding.payment.exception.ErrorCode.BAD_REQUEST_JSON;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import kcs.funding.payment.api.ResponseDto;
import kcs.funding.payment.common.dto.CommonSuccessDto;
import kcs.funding.payment.exception.CommonException;
import kcs.funding.payment.event.application.OutboxEventService;
import kcs.funding.payment.payment.application.PaymentExecutionCommand;
import kcs.funding.payment.payment.application.PaymentExecutionResult;
import kcs.funding.payment.payment.application.PaymentIntentOrchestrator;
import kcs.funding.payment.payment.domain.PaymentIntentType;
import kcs.funding.payment.order.application.PaymentOrderFinalizeService;
import kcs.funding.payment.order.dto.PaymentItemPayNowDto;
import kcs.funding.payment.order.dto.PaymentMyPayDto;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NativeOrderPaymentService {

    private final ObjectMapper objectMapper;
    private final NativePaymentBridgeClient nativePaymentBridgeClient;
    private final PaymentIntentOrchestrator paymentIntentOrchestrator;
    private final OutboxEventService outboxEventService;
    private final PaymentOrderFinalizeService paymentOrderFinalizeService;

    public boolean supports(HttpServletRequest request, boolean enabled) {
        if (!enabled || !"POST".equalsIgnoreCase(request.getMethod())) {
            return false;
        }
        String path = request.getRequestURI();
        return "/api/v1/pay/order".equals(path) || "/api/v1/pay/order/now".equals(path);
    }

    public ResponseEntity<byte[]> handle(HttpServletRequest request, HttpHeaders headers, byte[] body) {
        request.setAttribute("paymentServiceMode", "native-order");
        String path = request.getRequestURI();
        if ("/api/v1/pay/order/now".equals(path)) {
            return executeOrderNow(headers, body);
        }
        return executeOrder(headers, body);
    }

    private ResponseEntity<byte[]> executeOrderNow(HttpHeaders headers, byte[] body) {
        try {
            NativeOrderPreparationDto preparation = nativePaymentBridgeClient.prepareOrderNow(headers, body);
            PaymentExecutionResult paymentResult = execute(preparation, headers.getFirst("Idempotency-Key"), PaymentIntentType.ORDER_NOW);
            PaymentItemPayNowDto payload = readBody(body, PaymentItemPayNowDto.class);
            NativeOrderFinalizeResultDto finalizeResult = paymentOrderFinalizeService.finalizeOrderNow(
                    preparation.memberId(),
                    payload,
                    paymentResult.intentKey(),
                    preparation.totalAmount(),
                    preparation.pointAmount(),
                    preparation.pgAmount(),
                    preparation.fundingSupportedAmount()
            );
            boolean attached = paymentIntentOrchestrator.attachOrderIdIfAbsent(paymentResult.intentKey(), finalizeResult.orderId());
            if (attached) {
                outboxEventService.enqueuePaymentCompletedForOrder(
                        finalizeResult.orderId(),
                        finalizeResult.memberId(),
                        PaymentIntentType.ORDER_NOW,
                        preparation.referenceId(),
                        preparation.currency(),
                        finalizeResult.paymentIntentKey(),
                        finalizeResult.totalAmount(),
                        finalizeResult.pointAmount(),
                        finalizeResult.pgAmount(),
                        finalizeResult.fundingSupportedAmount(),
                        finalizeResult.sourceFundingId(),
                        paymentResult.pgProvider(),
                        paymentResult.pgTransactionId()
                );
                outboxEventService.enqueueOrderPaid(
                        finalizeResult.orderId(),
                        finalizeResult.memberId(),
                        PaymentIntentType.ORDER_NOW,
                        finalizeResult.paymentIntentKey(),
                        finalizeResult.totalAmount(),
                        finalizeResult.pointAmount(),
                        finalizeResult.pgAmount(),
                        finalizeResult.fundingSupportedAmount(),
                        finalizeResult.sourceFundingId()
                );
            }
            return successResponse();
        } catch (BackBridgeResponseException exception) {
            return bridgeFailureResponse(exception, "native-order");
        } catch (CommonException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("payment native order-now flow failed", exception);
        }
    }

    private ResponseEntity<byte[]> executeOrder(HttpHeaders headers, byte[] body) {
        try {
            NativeOrderPreparationDto preparation = nativePaymentBridgeClient.prepareOrder(headers, body);
            PaymentExecutionResult paymentResult = execute(preparation, headers.getFirst("Idempotency-Key"), PaymentIntentType.ORDER_CART);
            PaymentMyPayDto payload = readBody(body, PaymentMyPayDto.class);
            NativeOrderFinalizeResultDto finalizeResult = paymentOrderFinalizeService.finalizeOrder(
                    preparation.memberId(),
                    payload,
                    paymentResult.intentKey(),
                    preparation.totalAmount(),
                    preparation.pointAmount(),
                    preparation.pgAmount(),
                    preparation.fundingSupportedAmount()
            );
            boolean attached = paymentIntentOrchestrator.attachOrderIdIfAbsent(paymentResult.intentKey(), finalizeResult.orderId());
            if (attached) {
                outboxEventService.enqueuePaymentCompletedForOrder(
                        finalizeResult.orderId(),
                        finalizeResult.memberId(),
                        PaymentIntentType.ORDER_CART,
                        preparation.referenceId(),
                        preparation.currency(),
                        finalizeResult.paymentIntentKey(),
                        finalizeResult.totalAmount(),
                        finalizeResult.pointAmount(),
                        finalizeResult.pgAmount(),
                        finalizeResult.fundingSupportedAmount(),
                        finalizeResult.sourceFundingId(),
                        paymentResult.pgProvider(),
                        paymentResult.pgTransactionId()
                );
                outboxEventService.enqueueOrderPaid(
                        finalizeResult.orderId(),
                        finalizeResult.memberId(),
                        PaymentIntentType.ORDER_CART,
                        finalizeResult.paymentIntentKey(),
                        finalizeResult.totalAmount(),
                        finalizeResult.pointAmount(),
                        finalizeResult.pgAmount(),
                        finalizeResult.fundingSupportedAmount(),
                        finalizeResult.sourceFundingId()
                );
            }
            return successResponse();
        } catch (BackBridgeResponseException exception) {
            return bridgeFailureResponse(exception, "native-order");
        } catch (CommonException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalStateException("payment native order flow failed", exception);
        }
    }

    private PaymentExecutionResult execute(NativeOrderPreparationDto preparation, String idempotencyKey, PaymentIntentType paymentIntentType) {
        return paymentIntentOrchestrator.execute(
                new PaymentExecutionCommand(
                        preparation.memberId(),
                        paymentIntentType,
                        preparation.referenceId(),
                        idempotencyKey,
                        preparation.totalAmount(),
                        preparation.pointAmount(),
                        preparation.pgAmount(),
                        preparation.fundingSupportedAmount(),
                        preparation.currency()
                )
        );
    }

    private ResponseEntity<byte[]> successResponse() throws Exception {
        byte[] body = objectMapper.writeValueAsBytes(ResponseDto.ok(CommonSuccessDto.success()));
        HttpHeaders responseHeaders = new HttpHeaders();
        responseHeaders.set("X-Payment-Service-Mode", "native-order");
        return ResponseEntity.ok().headers(responseHeaders).body(body);
    }

    private <T> T readBody(byte[] body, Class<T> targetType) {
        try {
            return objectMapper.readValue(body == null ? new byte[0] : body, targetType);
        } catch (IOException exception) {
            throw new CommonException(BAD_REQUEST_JSON);
        }
    }

    private ResponseEntity<byte[]> bridgeFailureResponse(BackBridgeResponseException exception, String serviceMode) {
        HttpHeaders headers = new HttpHeaders();
        headers.putAll(exception.headers());
        headers.set("X-Payment-Service-Mode", serviceMode);
        return new ResponseEntity<>(exception.body(), headers, exception.statusCode());
    }
}
