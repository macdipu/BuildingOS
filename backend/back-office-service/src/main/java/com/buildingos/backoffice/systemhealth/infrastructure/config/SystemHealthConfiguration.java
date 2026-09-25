package com.buildingos.backoffice.systemhealth.infrastructure.config;

import com.buildingos.backoffice.systemhealth.application.getsystemhealth.GetSystemHealthService;
import com.buildingos.backoffice.systemhealth.application.getsystemhealth.GetSystemHealthUseCase;
import com.buildingos.backoffice.systemhealth.application.port.out.ServiceReadinessProbe;
import com.buildingos.backoffice.systemhealth.infrastructure.client.ActuatorReadinessProbe;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(HealthProperties.class)
public class SystemHealthConfiguration {
    @Bean
    ServiceReadinessProbe serviceReadinessProbe(HealthProperties properties, Environment environment) {
        return new ActuatorReadinessProbe(properties, environment);
    }

    @Bean
    GetSystemHealthUseCase getSystemHealth(ServiceReadinessProbe probe, Clock clock) {
        return new GetSystemHealthService(probe, clock);
    }
}
