package com.buildingos.building.outbox.infrastructure.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code buildingos.outbox.*}: the publisher worker (TECH-SPEC-F4 configuration impact). Topics listed are created
 * on startup because the brokers disable topic auto-creation.
 */
@ConfigurationProperties("buildingos.outbox")
public record OutboxProperties(Duration pollInterval, Integer batchSize, Duration sendTimeout,
        Duration retryBase, Duration retryMax, List<String> topics, Integer topicPartitions, Integer topicReplicas) {
    public OutboxProperties {
        pollInterval = pollInterval == null ? Duration.ofSeconds(1) : pollInterval;
        batchSize = batchSize == null ? 100 : batchSize;
        sendTimeout = sendTimeout == null ? Duration.ofSeconds(15) : sendTimeout;
        retryBase = retryBase == null ? Duration.ofSeconds(1) : retryBase;
        retryMax = retryMax == null ? Duration.ofMinutes(5) : retryMax;
        topics = topics == null ? List.of("ownership.transferred") : List.copyOf(topics);
        topicPartitions = topicPartitions == null ? 3 : topicPartitions;
        topicReplicas = topicReplicas == null ? 1 : topicReplicas;
    }
}
