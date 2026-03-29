package kcs.funding.payment.event.application;

import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(DomainEventPublisher.class)
public class LoggingDomainEventPublisher implements DomainEventPublisher {

    @Override
    public void publish(String topic, String key, String payload, Map<String, String> headers) {
        log.info("outbox publish(no-kafka): topic={}, key={}, payload={}", topic, key, payload);
    }

    @Override
    public String publisherName() {
        return "logging";
    }
}

