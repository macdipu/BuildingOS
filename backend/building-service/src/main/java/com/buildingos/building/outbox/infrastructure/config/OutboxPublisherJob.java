package com.buildingos.building.outbox.infrastructure.config;

import com.buildingos.building.outbox.application.publishpendingevents.PublishPendingEventsResult;
import com.buildingos.building.outbox.application.publishpendingevents.PublishPendingEventsUseCase;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;

/** Polls the outbox; a crashed pass leaves rows undelivered for the next one, never discarded. */
public class OutboxPublisherJob {
    private static final Logger LOG = LoggerFactory.getLogger(OutboxPublisherJob.class);
    private final PublishPendingEventsUseCase publish;
    private final Counter published;
    private final Counter failed;

    public OutboxPublisherJob(PublishPendingEventsUseCase publish, MeterRegistry meters) {
        this.publish = publish;
        this.published = meters.counter("buildingos.outbox.published");
        this.failed = meters.counter("buildingos.outbox.publish.failures");
    }

    @Scheduled(fixedDelayString = "${buildingos.outbox.poll-interval:PT1S}")
    public void run() {
        try {
            PublishPendingEventsResult result = publish.execute();
            published.increment(result.published());
            failed.increment(result.failed());
            if (result.failed() > 0) {
                LOG.warn("outbox publish pass: published={} failed={} deferred={}", result.published(),
                        result.failed(), result.deferred());
            }
        } catch (RuntimeException e) {
            LOG.error("outbox publish pass aborted; events stay pending", e);
        }
    }
}
