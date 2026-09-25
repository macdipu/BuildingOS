package com.buildingos.backoffice.systemhealth.infrastructure.client;

import com.buildingos.backoffice.systemhealth.application.port.out.ServiceReadinessProbe;
import com.buildingos.backoffice.systemhealth.domain.model.ServiceHealthStatus;
import com.buildingos.backoffice.systemhealth.domain.model.ServiceReadiness;
import com.buildingos.backoffice.systemhealth.infrastructure.config.HealthProperties;
import com.buildingos.backoffice.systemhealth.infrastructure.config.HealthProperties.MonitoredService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.core.env.Environment;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * {@code GET <service-url>/actuator/health/readiness} (public in every service via platform-web), no token relayed.
 * Only the HTTP code and latency are kept; the body is never read or forwarded (no component detail leak).
 */
public class ActuatorReadinessProbe implements ServiceReadinessProbe {
    private static final String READINESS = "/actuator/health/readiness";
    private final Map<String, MonitoredService> services = new LinkedHashMap<>();
    private final RestClient client;
    private final Environment environment;

    public ActuatorReadinessProbe(HealthProperties properties, Environment environment) {
        properties.services().forEach(service -> services.put(service.name(), service));
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.timeout());
        factory.setReadTimeout(properties.timeout());
        this.client = RestClient.builder().requestFactory(factory).build();
        this.environment = environment;
    }

    @Override
    public List<String> monitoredServices() {
        return List.copyOf(services.keySet());
    }

    @Override
    public ServiceReadiness probe(String serviceName) {
        long started = System.nanoTime();
        MonitoredService service = services.get(serviceName);
        String baseUrl = service == null ? null : baseUrl(service);
        if (baseUrl == null) {
            return ServiceReadiness.unknown(serviceName, 0);
        }
        try {
            int status = client.get().uri(stripTrailingSlash(baseUrl) + READINESS)
                    .exchange((request, response) -> response.getStatusCode().value());
            return new ServiceReadiness(serviceName,
                    status >= 200 && status < 300 ? ServiceHealthStatus.UP : ServiceHealthStatus.DOWN, status,
                    elapsedMs(started));
        } catch (RestClientException | IllegalArgumentException unreachable) {
            return ServiceReadiness.unknown(serviceName, elapsedMs(started));
        }
    }

    /** Self (no url) is reached on the port this server actually bound. */
    private String baseUrl(MonitoredService service) {
        if (!service.isSelf()) {
            return service.url();
        }
        String port = environment.getProperty("local.server.port");
        return port == null ? null : "http://127.0.0.1:" + port;
    }

    private static String stripTrailingSlash(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }

    private static long elapsedMs(long startedNanos) {
        return (System.nanoTime() - startedNanos) / 1_000_000;
    }
}
