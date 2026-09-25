package com.buildingos.building.audit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.building.audit.application.listauditevents.AuditEvent;
import com.buildingos.building.audit.application.listauditevents.ListAuditEventsQuery;
import com.buildingos.building.audit.application.listauditevents.ListAuditEventsService;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.NotPermittedException;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.model.EntityType;
import com.buildingos.building.shared.domain.model.LifecycleTransition;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import com.buildingos.building.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListAuditEventsServiceTest {
    private static final Actor SUPER = new Actor(UUID.randomUUID(), Set.of("SUPER_ADMIN"));
    private final UUID actor = UUID.randomUUID();
    private final UUID building = UUID.randomUUID();
    private final UUID application = UUID.randomUUID();
    private final List<Object[]> calls = new ArrayList<>();
    private final List<LifecycleTransition> transitionRows = List.of(
            new LifecycleTransition(UUID.randomUUID(), EntityType.BUILDING, building, "ONBOARDING", "ACTIVE", actor,
                    "Ready", Instant.parse("2026-09-25T12:00:00Z")),
            new LifecycleTransition(UUID.randomUUID(), EntityType.BUILDING_APPLICATION, application, null, "DRAFT",
                    actor, null, Instant.parse("2026-09-25T09:00:00Z")));
    private final List<AuditEntry> auditRows = List.of(
            new AuditEntry(UUID.randomUUID(), building, actor, "MEMBER_INVITED", "BUILDING_INVITATION",
                    UUID.randomUUID(), "Owner invited", Map.of("x", "1"), Map.of("y", "2"),
                    Instant.parse("2026-09-25T10:00:00Z")));
    private final LifecycleTransitionRepository transitions = new LifecycleTransitionRepository() {
        @Override public void append(LifecycleTransition t) { throw new UnsupportedOperationException(); }
        @Override public List<LifecycleTransition> findFor(EntityType type, UUID id) {
            throw new UnsupportedOperationException();
        }
        @Override public List<LifecycleTransition> list(Instant since, Instant until, String entityType, UUID by,
                int limit) {
            calls.add(new Object[] {"transition", since, until, entityType, by, limit});
            return transitionRows;
        }
    };
    private final AuditRepository audits = new AuditRepository() {
        @Override public void append(AuditEntry e) { throw new UnsupportedOperationException(); }
        @Override public List<AuditEntry> findByEntity(UUID b, String type, UUID id) {
            throw new UnsupportedOperationException();
        }
        @Override public List<AuditEntry> list(Instant since, Instant until, String entityType, UUID by, int limit) {
            calls.add(new Object[] {"audit", since, until, entityType, by, limit});
            return auditRows;
        }
    };
    private final ListAuditEventsService service = new ListAuditEventsService(transitions, audits);

    private List<AuditEvent> run(Actor caller, Integer limit) {
        return service.execute(caller, new ListAuditEventsQuery(null, null, null, null, limit));
    }

    @Test
    void mergesBothSourcesNewestFirstAndMapsWithoutPayloads() {
        var events = run(SUPER, null);
        assertThat(events).extracting(AuditEvent::occurredAt).containsExactly(
                Instant.parse("2026-09-25T12:00:00Z"), Instant.parse("2026-09-25T10:00:00Z"),
                Instant.parse("2026-09-25T09:00:00Z"));
        assertThat(events).allSatisfy(e -> assertThat(e.source()).isEqualTo("building-service"));

        AuditEvent activation = events.get(0);
        assertThat(activation.action()).isEqualTo("ONBOARDING→ACTIVE");
        assertThat(activation.entityType()).isEqualTo("BUILDING");
        assertThat(activation.entityId()).isEqualTo(building);
        assertThat(activation.buildingId()).isEqualTo(building);
        assertThat(activation.reason()).isEqualTo("Ready");

        AuditEvent invited = events.get(1);
        assertThat(invited.action()).isEqualTo("MEMBER_INVITED");
        assertThat(invited.entityType()).isEqualTo("BUILDING_INVITATION");
        assertThat(invited.buildingId()).isEqualTo(building);
        assertThat(invited.reason()).isEqualTo("Owner invited");

        AuditEvent created = events.get(2);
        assertThat(created.action()).isEqualTo("NONE→DRAFT");
        assertThat(created.entityType()).isEqualTo("BUILDING_APPLICATION");
        assertThat(created.entityId()).isEqualTo(application);
        assertThat(created.buildingId()).isNull();
        assertThat(created.actorUserId()).isEqualTo(actor);
    }

    @Test
    void truncatesMergedResultToLimitAndPassesFiltersToBothSources() {
        Instant since = Instant.parse("2026-09-01T00:00:00Z");
        Instant until = Instant.parse("2026-10-01T00:00:00Z");
        var events = service.execute(SUPER, new ListAuditEventsQuery(since, until, "BUILDING", actor, 2));
        assertThat(events).extracting(AuditEvent::occurredAt).containsExactly(
                Instant.parse("2026-09-25T12:00:00Z"), Instant.parse("2026-09-25T10:00:00Z"));
        assertThat(calls).hasSize(2);
        assertThat(calls.get(0)).containsExactly("transition", since, until, "BUILDING", actor, 2);
        assertThat(calls.get(1)).containsExactly("audit", since, until, "BUILDING", actor, 2);
    }

    @Test
    void defaultsLimitTo50AndRejectsOutOfRange() {
        run(SUPER, null);
        assertThat(calls.get(0)[5]).isEqualTo(50);
        run(SUPER, 200);
        assertThat(calls.get(2)[5]).isEqualTo(200);
        calls.clear();
        assertThatThrownBy(() -> run(SUPER, 201)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> run(SUPER, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(calls).isEmpty();
    }

    @Test
    void onlySuperAdminMayRead() {
        for (Set<String> roles : List.of(Set.<String>of(), Set.of("PLATFORM_ADMIN"), Set.of("SUPPORT_AGENT"))) {
            assertThatThrownBy(() -> run(new Actor(UUID.randomUUID(), roles), null))
                    .isInstanceOf(NotPermittedException.class);
        }
        assertThat(calls).isEmpty();
    }
}
