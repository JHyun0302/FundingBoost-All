package kcs.funding.fundingboost.saga.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kcs.funding.fundingboost.saga.application.CommerceOrderPaidSagaHandler;
import kcs.funding.fundingboost.saga.application.FundingContributionCreatedSagaHandler;
import kcs.funding.fundingboost.saga.application.PaymentFailedSagaHandler;
import kcs.funding.fundingboost.saga.inbox.InboxEventService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.inbox.consumers", name = "enabled", havingValue = "true")
public class DomainSagaConsumer {

    private static final String COMMERCE_ORDER_PAID_CONSUMER = "commerce-order-paid";
    private static final String FUNDING_CONTRIBUTION_CONSUMER = "funding-contribution-created";
    private static final String PAYMENT_FAILED_CONSUMER = "payment-failed";

    private final ObjectMapper objectMapper;
    private final InboxEventService inboxEventService;
    private final CommerceOrderPaidSagaHandler commerceOrderPaidSagaHandler;
    private final FundingContributionCreatedSagaHandler fundingContributionCreatedSagaHandler;
    private final PaymentFailedSagaHandler paymentFailedSagaHandler;

    @KafkaListener(
            topics = "${app.inbox.topics.order-paid}",
            groupId = "${app.inbox.consumers.group-id}"
    )
    public void onCommerceOrderPaid(ConsumerRecord<String, String> record) {
        consume(record, COMMERCE_ORDER_PAID_CONSUMER, "commerce.order.paid.v1", commerceOrderPaidSagaHandler::handle);
    }

    @KafkaListener(
            topics = "${app.inbox.topics.funding-contribution-created}",
            groupId = "${app.inbox.consumers.group-id}"
    )
    public void onFundingContributionCreated(ConsumerRecord<String, String> record) {
        consume(record, FUNDING_CONTRIBUTION_CONSUMER, "funding.contribution.created.v1",
                fundingContributionCreatedSagaHandler::handle);
    }

    @KafkaListener(
            topics = "${app.inbox.topics.payment-failed}",
            groupId = "${app.inbox.consumers.group-id}"
    )
    public void onPaymentFailed(ConsumerRecord<String, String> record) {
        consume(record, PAYMENT_FAILED_CONSUMER, "payment.failed.v1", paymentFailedSagaHandler::handle);
    }

    private void consume(
            ConsumerRecord<String, String> record,
            String consumerName,
            String eventType,
            SagaPayloadHandler handler
    ) {
        try {
            JsonNode payload = objectMapper.readTree(record.value());
            boolean consumed = inboxEventService.consumeOnce(
                    consumerName,
                    record.topic(),
                    record.key(),
                    eventType,
                    record.value(),
                    () -> handler.handle(payload)
            );
            if (!consumed) {
                log.info("skip already consumed inbox event: consumer={}, topic={}, key={}",
                        consumerName, record.topic(), record.key());
            }
        } catch (Exception exception) {
            log.warn("failed to consume saga inbox event: consumer={}, topic={}, key={}",
                    consumerName, record.topic(), record.key(), exception);
            throw new IllegalStateException("failed to consume saga inbox event", exception);
        }
    }

    @FunctionalInterface
    private interface SagaPayloadHandler {
        void handle(JsonNode payload);
    }
}
