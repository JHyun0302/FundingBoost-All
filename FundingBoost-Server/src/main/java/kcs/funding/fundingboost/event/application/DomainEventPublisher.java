package kcs.funding.fundingboost.event.application;

import java.util.Map;

public interface DomainEventPublisher {

    void publish(String topic, String key, String payload, Map<String, String> headers);

    String publisherName();
}

