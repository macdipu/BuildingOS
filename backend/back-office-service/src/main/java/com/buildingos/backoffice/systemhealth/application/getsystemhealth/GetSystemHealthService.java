package com.buildingos.backoffice.systemhealth.application.getsystemhealth;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.NotPermittedException;
import com.buildingos.backoffice.systemhealth.application.port.out.ServiceReadinessProbe;
import com.buildingos.backoffice.systemhealth.domain.model.ServiceReadiness;
import com.buildingos.backoffice.systemhealth.domain.model.SystemHealth;
import java.time.Clock;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** BOC-08: SUPER_ADMIN only; probes every configured service in parallel and rolls the results up. */
public final class GetSystemHealthService implements GetSystemHealthUseCase {
    private final ServiceReadinessProbe probe;
    private final Clock clock;

    public GetSystemHealthService(ServiceReadinessProbe probe, Clock clock) {
        this.probe = probe;
        this.clock = clock;
    }

    @Override
    public SystemHealth execute(Actor actor) {
        if (!actor.isSuperAdmin()) {
            throw new NotPermittedException();
        }
        List<String> services = probe.monitoredServices();
        if (services.isEmpty()) {
            return SystemHealth.of(clock.instant(), List.of());
        }
        ExecutorService pool = Executors.newFixedThreadPool(services.size());
        try {
            List<CompletableFuture<ServiceReadiness>> checks = services.stream()
                    .map(name -> CompletableFuture.supplyAsync(() -> probe.probe(name), pool)
                            .exceptionally(failed -> ServiceReadiness.unknown(name, 0)))
                    .toList();
            List<ServiceReadiness> results = checks.stream().map(CompletableFuture::join).toList();
            return SystemHealth.of(clock.instant(), results);
        } finally {
            pool.shutdownNow();
        }
    }
}
