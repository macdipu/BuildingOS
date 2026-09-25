package com.buildingos.subscription.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.subscription.audit.application.listauditevents.AuditEvent;
import com.buildingos.subscription.audit.application.listauditevents.ListAuditEventsQuery;
import com.buildingos.subscription.audit.application.listauditevents.ListAuditEventsService;
import com.buildingos.subscription.audit.domain.model.AuditRecord;
import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.shared.application.NotPermittedException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListAuditEventsServiceTest {
    private static final Actor SUPER = new Actor(UUID.randomUUID(), Set.of("SUPER_ADMIN"));
    private final UUID actor = UUID.randomUUID();
    private final List<Object[]> calls = new ArrayList<>();
    private final List<AuditRecord> rows = List.of(
            new AuditRecord(UUID.randomUUID(), actor, "PLAN_CREATED", "SUBSCRIPTION_PLAN", "PREMIUM",
                    Instant.parse("2026-09-25T10:00:00Z")));
    private final ListAuditEventsService service = new ListAuditEventsService(
            (since, until, entityType, by, limit) -> {
                calls.add(new Object[] {since, until, entityType, by, limit});
                return rows;
            });

    private List<AuditEvent> run(Actor caller, Integer limit) {
        return service.execute(caller, new ListAuditEventsQuery(null, null, null, null, limit));
    }

    @Test
    void mapsRowsToCommonItemsWithoutPayloads() {
        AuditEvent event = run(SUPER, null).get(0);
        assertThat(event.source()).isEqualTo("subscription-service");
        assertThat(event.action()).isEqualTo("PLAN_CREATED");
        assertThat(event.entityType()).isEqualTo("SUBSCRIPTION_PLAN");
        assertThat(event.entityId()).isEqualTo("PREMIUM");
        assertThat(event.actorUserId()).isEqualTo(actor);
        assertThat(event.buildingId()).isNull();
        assertThat(event.reason()).isNull();
        assertThat(event.occurredAt()).isEqualTo(Instant.parse("2026-09-25T10:00:00Z"));
    }

    @Test
    void passesFiltersAndDefaultsLimitTo50() {
        Instant since = Instant.parse("2026-09-01T00:00:00Z");
        Instant until = Instant.parse("2026-10-01T00:00:00Z");
        service.execute(SUPER, new ListAuditEventsQuery(since, until, "SUBSCRIPTION_PLAN", actor, null));
        assertThat(calls.get(0)).containsExactly(since, until, "SUBSCRIPTION_PLAN", actor, 50);
        run(SUPER, 200);
        assertThat(calls.get(1)[4]).isEqualTo(200);
    }

    @Test
    void rejectsLimitOutOfRange() {
        assertThatThrownBy(() -> run(SUPER, 201)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> run(SUPER, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(calls).isEmpty();
    }

    @Test
    void onlySuperAdminMayRead() {
        for (Set<String> roles : List.of(Set.<String>of(), Set.of("PLATFORM_ADMIN"), Set.of("SUBSCRIPTION_ADMIN"))) {
            assertThatThrownBy(() -> run(new Actor(UUID.randomUUID(), roles), null))
                    .isInstanceOf(NotPermittedException.class);
        }
        assertThat(calls).isEmpty();
    }
}
