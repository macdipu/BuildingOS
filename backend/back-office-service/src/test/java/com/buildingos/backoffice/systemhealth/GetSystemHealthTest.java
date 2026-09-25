package com.buildingos.backoffice.systemhealth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.NotPermittedException;
import com.buildingos.backoffice.systemhealth.application.getsystemhealth.GetSystemHealthService;
import com.buildingos.backoffice.systemhealth.application.port.out.ServiceReadinessProbe;
import com.buildingos.backoffice.systemhealth.domain.model.OverallHealth;
import com.buildingos.backoffice.systemhealth.domain.model.ServiceHealthStatus;
import com.buildingos.backoffice.systemhealth.domain.model.ServiceReadiness;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** F6-T5a (BOC-08): aggregation rule and SUPER_ADMIN gate. */
class GetSystemHealthTest {
    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private static Actor actor(String... roles) {
        return new Actor(UUID.randomUUID(), Set.of(roles));
    }

    private static ServiceReadinessProbe probe(Map<String, ServiceHealthStatus> statuses) {
        return new ServiceReadinessProbe() {
            @Override
            public List<String> monitoredServices() {
                return List.copyOf(statuses.keySet());
            }

            @Override
            public ServiceReadiness probe(String name) {
                ServiceHealthStatus status = statuses.get(name);
                if (status == null) {
                    throw new IllegalStateException("probe blew up");
                }
                return new ServiceReadiness(name, status, status == ServiceHealthStatus.UNKNOWN ? null : 200, 5);
            }
        };
    }

    @Test
    void overallIsUpOnlyWhenEveryServiceIsUp() {
        var up = ServiceHealthStatus.UP;
        var down = ServiceHealthStatus.DOWN;
        var unknown = ServiceHealthStatus.UNKNOWN;
        assertThat(OverallHealth.of(List.of(up, up, up))).isEqualTo(OverallHealth.UP);
        assertThat(OverallHealth.of(List.of(up, down, up))).isEqualTo(OverallHealth.DEGRADED);
        assertThat(OverallHealth.of(List.of(up, unknown))).isEqualTo(OverallHealth.DEGRADED);
        assertThat(OverallHealth.of(List.of(down, unknown))).isEqualTo(OverallHealth.DOWN);
        assertThat(OverallHealth.of(List.of(down))).isEqualTo(OverallHealth.DOWN);
        assertThat(OverallHealth.of(List.of())).isEqualTo(OverallHealth.DOWN);
    }

    @Test
    void superAdminGetsEveryServiceInConfiguredOrder() {
        var statuses = new java.util.LinkedHashMap<String, ServiceHealthStatus>();
        statuses.put("auth-service", ServiceHealthStatus.UP);
        statuses.put("building-service", ServiceHealthStatus.DOWN);
        statuses.put("back-office-service", ServiceHealthStatus.UP);
        var health = new GetSystemHealthService(probe(statuses), CLOCK).execute(actor("SUPER_ADMIN"));
        assertThat(health.checkedAt()).isEqualTo(NOW);
        assertThat(health.overall()).isEqualTo(OverallHealth.DEGRADED);
        assertThat(health.services()).extracting(ServiceReadiness::name)
                .containsExactly("auth-service", "building-service", "back-office-service");
    }

    @Test
    void failingProbeBecomesUnknownInsteadOfFailingTheCall() {
        var service = new GetSystemHealthService(new ServiceReadinessProbe() {
            @Override
            public List<String> monitoredServices() {
                return List.of("auth-service");
            }

            @Override
            public ServiceReadiness probe(String name) {
                throw new IllegalStateException("probe blew up");
            }
        }, CLOCK);
        var health = service.execute(actor("SUPER_ADMIN"));
        assertThat(health.services()).singleElement().satisfies(readiness -> {
            assertThat(readiness.status()).isEqualTo(ServiceHealthStatus.UNKNOWN);
            assertThat(readiness.httpStatus()).isNull();
        });
        assertThat(health.overall()).isEqualTo(OverallHealth.DOWN);
    }

    @Test
    void nonSuperAdminIsRejected() {
        var service = new GetSystemHealthService(probe(Map.of("auth-service", ServiceHealthStatus.UP)), CLOCK);
        for (String role : List.of("PLATFORM_ADMIN", "SUPPORT_AGENT", "ONBOARDING_AGENT")) {
            assertThatThrownBy(() -> service.execute(actor(role))).isInstanceOf(NotPermittedException.class);
        }
        assertThatThrownBy(() -> service.execute(actor())).isInstanceOf(NotPermittedException.class);
    }
}
