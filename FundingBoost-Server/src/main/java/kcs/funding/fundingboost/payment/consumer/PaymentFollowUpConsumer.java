package kcs.funding.fundingboost.payment.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import kcs.funding.fundingboost.payment.application.followup.PaymentCompletedFollowUpDispatcher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.payment.follow-up.consumers", name = "enabled", havingValue = "true")
public class PaymentFollowUpConsumer {

    private final ObjectMapper objectMapper;
    private final PaymentCompletedFollowUpDispatcher paymentCompletedFollowUpDispatcher;

    @KafkaListener(
            topics = "${app.payment.topics.completed}",
            groupId = "${app.payment.follow-up.consumers.group-id}"
    )
    public void onPaymentCompleted(ConsumerRecord<String, String> record) {
        try {
            JsonNode payload = objectMapper.readTree(record.value());
            paymentCompletedFollowUpDispatcher.dispatch(payload);
        } catch (Exception exception) {
            log.warn("failed to process payment completed follow-up event: key={}", record.key(), exception);
        }
    }

    @KafkaListener(
            topics = "${app.payment.topics.failed}",
            groupId = "${app.payment.follow-up.consumers.group-id}"
    )
    public void onPaymentFailed(ConsumerRecord<String, String> record) {
        log.warn("payment failed follow-up event received: key={}, payload={}", record.key(), record.value());
    }
}
