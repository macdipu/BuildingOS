package com.buildingos.building.outbox.infrastructure.config;

import com.buildingos.building.outbox.application.port.out.EventPublisher;
import com.buildingos.building.outbox.application.publishpendingevents.PublishPendingEventsService;
import com.buildingos.building.outbox.application.publishpendingevents.PublishPendingEventsUseCase;
import com.buildingos.building.outbox.infrastructure.messaging.KafkaEventPublisher;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.OutboxBacklog;
import com.buildingos.building.shared.domain.repository.OutboxRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.MeterBinder;
import java.time.Clock;
import java.time.Duration;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.KafkaAdmin;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OutboxProperties.class)
@ConditionalOnBooleanProperty(name = "buildingos.outbox.enabled", matchIfMissing = true)
@EnableScheduling
public class OutboxConfiguration {
    @Bean
    EventPublisher eventPublisher(KafkaTemplate<String, String> kafka, OutboxProperties properties) {
        return new KafkaEventPublisher(kafka, properties.sendTimeout());
    }

    @Bean
    PublishPendingEventsUseCase publishPendingEvents(OutboxRepository outbox, EventPublisher publisher,
            UnitOfWork uow, Clock clock, OutboxProperties properties) {
        return new PublishPendingEventsService(outbox, publisher, uow, clock, properties.batchSize(),
                properties.retryBase(), properties.retryMax());
    }

    @Bean
    OutboxPublisherJob outboxPublisherJob(PublishPendingEventsUseCase publish, MeterRegistry meters) {
        return new OutboxPublisherJob(publish, meters);
    }

    @Bean
    KafkaAdmin.NewTopics outboxTopics(OutboxProperties properties) {
        return new KafkaAdmin.NewTopics(properties.topics().stream()
                .map(topic -> TopicBuilder.name(topic).partitions(properties.topicPartitions())
                        .replicas(properties.topicReplicas()).build())
                .toArray(NewTopic[]::new));
    }

    @Bean
    MeterBinder outboxBacklogMetrics(OutboxRepository outbox, Clock clock) {
        return registry -> {
            Gauge.builder("buildingos.outbox.pending", outbox, o -> o.backlog().pending()).register(registry);
            Gauge.builder("buildingos.outbox.oldest.age", outbox, o -> oldestAgeSeconds(o.backlog(), clock))
                    .baseUnit("seconds").register(registry);
        };
    }

    private static double oldestAgeSeconds(OutboxBacklog backlog, Clock clock) {
        return backlog.oldestOccurredAt().map(at -> Duration.between(at, clock.instant()).toMillis() / 1000.0)
                .orElse(0.0);
    }
}
