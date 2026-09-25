package com.buildingos.building.audit.infrastructure.config;

import com.buildingos.building.audit.application.listauditevents.ListAuditEventsService;
import com.buildingos.building.audit.application.listauditevents.ListAuditEventsUseCase;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import com.buildingos.building.shared.domain.repository.LifecycleTransitionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class AuditConfiguration {
    @Bean
    ListAuditEventsUseCase listAuditEvents(LifecycleTransitionRepository transitions, AuditRepository audits) {
        return new ListAuditEventsService(transitions, audits);
    }
}
