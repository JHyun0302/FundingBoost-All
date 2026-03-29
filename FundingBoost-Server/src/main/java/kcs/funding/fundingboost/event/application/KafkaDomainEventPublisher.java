package kcs.funding.fundingboost.event.application;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true")
public class KafkaDomainEventPublisher implements DomainEventPublisher {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Override
    public void publish(String topic, String key, String payload, Map<String, String> headers) {
        ProducerRecord<String, String> record = new ProducerRecord<>(topic, key, payload);

        if (headers != null) {
            headers.forEach((headerKey, headerValue) -> {
                if (headerValue == null) {
                    return;
                }
                record.headers().add(headerKey, headerValue.getBytes(StandardCharsets.UTF_8));
            });
        }

        try {
            kafkaTemplate.send(record).get(10, TimeUnit.SECONDS);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("kafka publish interrupted", exception);
        } catch (ExecutionException | TimeoutException exception) {
            throw new IllegalStateException("kafka publish failed", exception);
        }
    }

    @Override
    public String publisherName() {
        return "kafka";
    }
}

