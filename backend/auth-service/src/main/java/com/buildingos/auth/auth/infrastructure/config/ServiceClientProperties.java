package com.buildingos.auth.auth.infrastructure.config;

import com.buildingos.auth.auth.infrastructure.security.ConfiguredServiceClientRegistry.ClientConfig;
import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code buildingos.auth.service.*}: service clients allowed to exchange credentials for a token (D-38). */
@ConfigurationProperties("buildingos.auth.service")
public record ServiceClientProperties(Map<String, ClientConfig> clients, Duration tokenTtl) {
    public ServiceClientProperties {
        clients = clients == null ? Map.of() : Map.copyOf(clients);
        tokenTtl = tokenTtl == null ? Duration.ofMinutes(5) : tokenTtl;
    }
}
