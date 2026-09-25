package com.buildingos.subscription.audit.infrastructure.config;

import com.buildingos.subscription.audit.application.listauditevents.ListAuditEventsService;
import com.buildingos.subscription.audit.application.listauditevents.ListAuditEventsUseCase;
import com.buildingos.subscription.audit.domain.repository.AuditEventRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AuditConfiguration {
    @Bean
    ListAuditEventsUseCase listAuditEvents(AuditEventRepository audits) {
        return new ListAuditEventsService(audits);
    }
}
