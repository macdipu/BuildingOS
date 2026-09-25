package com.buildingos.backoffice.auditlog.infrastructure.config;

import com.buildingos.backoffice.auditlog.application.listauditevents.ListAuditEventsService;
import com.buildingos.backoffice.auditlog.application.listauditevents.ListAuditEventsUseCase;
import com.buildingos.backoffice.auditlog.application.port.out.RemoteAuditLog;
import com.buildingos.backoffice.auditlog.infrastructure.client.HttpRemoteAuditLog;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import com.buildingos.backoffice.shared.infrastructure.client.ServiceClientProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.concurrent.DelegatingSecurityContextRunnable;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AuditProperties.class)
public class AuditLogConfiguration {
    @Bean
    RemoteAuditLog remoteAuditLog(ServiceClientProperties services, AuditProperties properties) {
        return new HttpRemoteAuditLog(services, properties.timeout());
    }

    /** Fan-out tasks carry the caller's security context so BearerTokenRelay forwards the token. */
    @Bean
    ListAuditEventsUseCase listAuditEvents(RemoteAuditLog remote, LifecycleTransitionRepository transitions,
            AuditProperties properties) {
        return new ListAuditEventsService(remote, transitions, properties.timeout(),
                DelegatingSecurityContextRunnable::new);
    }
}
