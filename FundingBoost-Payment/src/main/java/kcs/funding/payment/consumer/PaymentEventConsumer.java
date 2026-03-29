package kcs.funding.payment.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "app.payment.consumers", name = "enabled", havingValue = "true")
public class PaymentEventConsumer {

    @KafkaListener(
            topics = "${app.payment.topics.completed}",
            groupId = "${app.payment.consumers.group-id}"
    )
    public void onPaymentCompleted(
            String payload,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.info("payment consumed completed event: key={}, payload={}", key, payload);
    }

    @KafkaListener(
            topics = "${app.payment.topics.failed}",
            groupId = "${app.payment.consumers.group-id}"
    )
    public void onPaymentFailed(
            String payload,
            @Header(name = KafkaHeaders.RECEIVED_KEY, required = false) String key
    ) {
        log.warn("payment consumed failed event: key={}, payload={}", key, payload);
    }
}
