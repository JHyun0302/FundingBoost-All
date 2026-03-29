package kcs.funding.fundingboost.event.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import kcs.funding.fundingboost.domain.entity.Order;
import kcs.funding.fundingboost.event.domain.OutboxEvent;
import kcs.funding.fundingboost.event.domain.OutboxEventRepository;
import kcs.funding.fundingboost.payment.domain.PaymentIntentType;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class OutboxEventService {

    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Value("${app.outbox.topic-prefix:fundingboost}")
    private String topicPrefix;

    public void enqueue(OutboxMessage message) {
        outboxEventRepository.save(OutboxEvent.create(
                message.topic(),
                message.eventKey(),
                message.eventType(),
                message.aggregateType(),
                message.aggregateId(),
                toJson(message.payload()),
                toJson(message.headers() == null ? Map.of() : message.headers()),
                LocalDateTime.now()
        ));
    }

    public void enqueueIfAbsent(OutboxMessage message) {
        if (outboxEventRepository.existsByEventTypeAndEventKey(message.eventType(), message.eventKey())) {
            return;
        }
        enqueue(message);
    }

    public void enqueueFundingContributionCreated(
            Long fundingId,
            Long contributorId,
            Long contributorMemberId,
            Long fundingOwnerMemberId,
            int fundingPrice,
            int usingPoint,
            int collectPrice,
            int totalPrice
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("fundingId", fundingId);
        payload.put("contributorId", contributorId);
        payload.put("contributorMemberId", contributorMemberId);
        payload.put("fundingOwnerMemberId", fundingOwnerMemberId);
        payload.put("fundingPrice", fundingPrice);
        payload.put("usingPoint", usingPoint);
        payload.put("collectPrice", collectPrice);
        payload.put("totalPrice", totalPrice);

        enqueueIfAbsent(new OutboxMessage(
                topic("funding.contribution.created.v1"),
                fundingId + ":" + contributorId,
                "funding.contribution.created.v1",
                "FundingContribution",
                String.valueOf(contributorId),
                payload,
                Map.of("schemaVersion", "v1")
        ));
    }

    public void enqueueOrderPaid(Order order, Long memberId, PaymentIntentType paymentIntentType) {
        enqueueOrderPaid(
                order.getOrderId(),
                memberId,
                paymentIntentType.name(),
                order.getPaymentIntentKey(),
                order.getTotalPrice(),
                order.getPointUsedAmount(),
                order.getDirectPaidAmount(),
                order.getFundingSupportedAmount(),
                order.getSourceFundingId()
        );
    }

    public void enqueueOrderPaid(
            Long orderId,
            Long memberId,
            String paymentIntentType,
            String paymentIntentKey,
            int totalPrice,
            int pointUsedAmount,
            int directPaidAmount,
            int fundingSupportedAmount,
            Long sourceFundingId
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("orderId", orderId);
        payload.put("memberId", memberId);
        payload.put("paymentIntentType", paymentIntentType);
        payload.put("paymentIntentKey", paymentIntentKey);
        payload.put("totalPrice", totalPrice);
        payload.put("pointUsedAmount", pointUsedAmount);
        payload.put("directPaidAmount", directPaidAmount);
        payload.put("fundingSupportedAmount", fundingSupportedAmount);
        payload.put("sourceFundingId", sourceFundingId);

        enqueueIfAbsent(new OutboxMessage(
                topic("commerce.order.paid.v1"),
                String.valueOf(orderId),
                "commerce.order.paid.v1",
                "Order",
                String.valueOf(orderId),
                payload,
                Map.of("schemaVersion", "v1")
        ));
    }

    public void enqueuePaymentCompletedForOrder(
            Order order,
            Long memberId,
            PaymentIntentType paymentIntentType,
            Long referenceId,
            String currency,
            String pgProvider,
            String pgTransactionId
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentFlow", paymentIntentType.name());
        payload.put("paymentIntentKey", order.getPaymentIntentKey());
        payload.put("memberId", memberId);
        payload.put("referenceId", referenceId);
        payload.put("orderId", order.getOrderId());
        payload.put("currency", currency);
        payload.put("totalAmount", order.getTotalPrice());
        payload.put("pointAmount", order.getPointUsedAmount());
        payload.put("pgAmount", order.getDirectPaidAmount());
        payload.put("fundingSupportedAmount", order.getFundingSupportedAmount());
        payload.put("sourceFundingId", order.getSourceFundingId());
        payload.put("pgProvider", pgProvider);
        payload.put("pgTransactionId", pgTransactionId);

        enqueueIfAbsent(new OutboxMessage(
                topic("payment.completed.v1"),
                order.getPaymentIntentKey(),
                "payment.completed.v1",
                "PaymentIntent",
                order.getPaymentIntentKey(),
                payload,
                Map.of("schemaVersion", "v1")
        ));
    }

    public void enqueuePaymentCompletedForFundingContribution(
            Long fundingId,
            Long contributorId,
            Long contributorMemberId,
            Long fundingOwnerMemberId,
            int fundingPrice,
            int usingPoint,
            int collectPrice,
            int totalPrice
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentFlow", "FRIEND_FUNDING");
        payload.put("fundingId", fundingId);
        payload.put("contributorId", contributorId);
        payload.put("contributorMemberId", contributorMemberId);
        payload.put("fundingOwnerMemberId", fundingOwnerMemberId);
        payload.put("fundingPrice", fundingPrice);
        payload.put("usingPoint", usingPoint);
        payload.put("collectPrice", collectPrice);
        payload.put("totalPrice", totalPrice);

        enqueueIfAbsent(new OutboxMessage(
                topic("payment.completed.v1"),
                "friend-funding:" + fundingId + ":" + contributorId,
                "payment.completed.v1",
                "FundingContribution",
                String.valueOf(contributorId),
                payload,
                Map.of("schemaVersion", "v1")
        ));
    }

    public void enqueuePaymentFailed(
            Long memberId,
            PaymentIntentType paymentIntentType,
            Long referenceId,
            String paymentIntentKey,
            int totalAmount,
            int pointAmount,
            int pgAmount,
            int fundingSupportedAmount,
            String currency,
            String errorCode,
            String errorMessage
    ) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("paymentFlow", paymentIntentType.name());
        payload.put("paymentIntentKey", paymentIntentKey);
        payload.put("memberId", memberId);
        payload.put("referenceId", referenceId);
        payload.put("currency", currency);
        payload.put("totalAmount", totalAmount);
        payload.put("pointAmount", pointAmount);
        payload.put("pgAmount", pgAmount);
        payload.put("fundingSupportedAmount", fundingSupportedAmount);
        payload.put("errorCode", errorCode);
        payload.put("errorMessage", errorMessage);

        enqueueIfAbsent(new OutboxMessage(
                topic("payment.failed.v1"),
                paymentIntentKey,
                "payment.failed.v1",
                "PaymentIntent",
                paymentIntentKey,
                payload,
                Map.of("schemaVersion", "v1")
        ));
    }

    private String topic(String suffix) {
        return topicPrefix + "." + suffix;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("failed to serialize outbox payload", exception);
        }
    }
}
