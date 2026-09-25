package com.buildingos.backoffice.auditlog;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.backoffice.auditlog.application.listauditevents.ListAuditEventsQuery;
import com.buildingos.backoffice.auditlog.application.listauditevents.ListAuditEventsService;
import com.buildingos.backoffice.auditlog.application.port.out.RemoteAuditLog;
import com.buildingos.backoffice.auditlog.domain.model.AuditEvent;
import com.buildingos.backoffice.auditlog.domain.model.AuditSource;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.NotPermittedException;
import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

/** F6-T5d: fan-out, per-source failure/timeout, source and entityType filters, SUPER_ADMIN gate, limit. */
class ListAuditEventsTest {
    private static final Instant T0 = Instant.parse("2026-09-25T10:00:00Z");
    private final Set<AuditSource> failing = ConcurrentHashMap.newKeySet();
    private final Set<AuditSource> slow = ConcurrentHashMap.newKeySet();
    private final Set<AuditSource> called = ConcurrentHashMap.newKeySet();
    private final AtomicReference<EntityType> ownTypeFilter = new AtomicReference<>();
    private final LifecycleTransition created = new LifecycleTransition(UUID.randomUUID(), EntityType.SUPPORT_SESSION,
            UUID.randomUUID(), null, "ACTIVE", UUID.randomUUID(), "help", T0.plusSeconds(5));

    private final RemoteAuditLog remote = (source, filter) -> {
        called.add(source);
        if (failing.contains(source)) {
            throw new IllegalStateException("down");
        }
        if (slow.contains(source)) {
            try {
                Thread.sleep(2000);
            } catch (InterruptedException interrupted) {
                Thread.currentThread().interrupt();
            }
        }
        return List.of(new AuditEvent(UUID.randomUUID(), source, T0.plusSeconds(source.ordinal()), null, "A", "X",
                "e", null, null, null));
    };

    private final LifecycleTransitionRepository transitions = new LifecycleTransitionRepository() {
        @Override public void append(LifecycleTransition transition) { }
        @Override public List<LifecycleTransition> findFor(EntityType type, UUID entityId) { return List.of(); }
        @Override public List<LifecycleTransition> list(Instant since, Instant until, EntityType type,
                UUID actorUserId, int limit) {
            called.add(AuditSource.BACK_OFFICE_SERVICE);
            ownTypeFilter.set(type);
            return List.of(created);
        }
    };

    private final ListAuditEventsService service = new ListAuditEventsService(remote, transitions,
            Duration.ofMillis(300), task -> task);

    private static Actor actor(String... roles) {
        return new Actor(UUID.randomUUID(), Set.of(roles));
    }

    private static ListAuditEventsQuery query(AuditSource source, String entityType, Integer limit) {
        return new ListAuditEventsQuery(null, null, source, entityType, null, limit);
    }

    @Test
    void mergesAllSourcesIncludingOwnTransitions() {
        var page = service.execute(actor("SUPER_ADMIN"), query(null, null, null));
        assertThat(called).containsExactlyInAnyOrder(AuditSource.values());
        assertThat(page.items()).extracting(AuditEvent::source).containsExactly(AuditSource.BACK_OFFICE_SERVICE,
                AuditSource.SUBSCRIPTION_SERVICE, AuditSource.BUILDING_SERVICE, AuditSource.AUTH_SERVICE);
        AuditEvent own = page.items().get(0);
        assertThat(own.action()).isEqualTo("NONE→ACTIVE");
        assertThat(own.entityType()).isEqualTo("SUPPORT_SESSION");
        assertThat(own.entityId()).isEqualTo(created.entityId().toString());
        assertThat(own.reason()).isEqualTo("help");
        assertThat(own.buildingId()).isNull();
        assertThat(page.unavailableSources()).isEmpty();
        assertThat(page.nextUntil()).isNull();
    }

    @Test
    void failedAndTimedOutSourcesAreUnavailableOthersStillReturned() {
        failing.add(AuditSource.BUILDING_SERVICE);
        slow.add(AuditSource.AUTH_SERVICE);
        var page = service.execute(actor("SUPER_ADMIN"), query(null, null, 2));
        assertThat(page.unavailableSources()).containsExactly(AuditSource.AUTH_SERVICE, AuditSource.BUILDING_SERVICE);
        assertThat(page.items()).extracting(AuditEvent::source).containsExactly(AuditSource.BACK_OFFICE_SERVICE,
                AuditSource.SUBSCRIPTION_SERVICE);
        assertThat(page.nextUntil()).isEqualTo(T0.plusSeconds(AuditSource.SUBSCRIPTION_SERVICE.ordinal()));
    }

    @Test
    void sourceFilterQueriesOnlyThatSource() {
        var page = service.execute(actor("SUPER_ADMIN"), query(AuditSource.SUBSCRIPTION_SERVICE, null, null));
        assertThat(called).containsExactly(AuditSource.SUBSCRIPTION_SERVICE);
        assertThat(page.items()).hasSize(1);
    }

    @Test
    void ownSourceMatchesOnlyItsEntityTypes() {
        var foreign = service.execute(actor("SUPER_ADMIN"), query(AuditSource.BACK_OFFICE_SERVICE, "BUILDING", null));
        assertThat(foreign.items()).isEmpty();
        assertThat(called).isEmpty();
        service.execute(actor("SUPER_ADMIN"), query(AuditSource.BACK_OFFICE_SERVICE, "SUPPORT_SESSION", null));
        assertThat(ownTypeFilter.get()).isEqualTo(EntityType.SUPPORT_SESSION);
    }

    @Test
    void onlySuperAdminAndLimitWithinBounds() {
        assertThatThrownBy(() -> service.execute(actor("PLATFORM_ADMIN"), query(null, null, null)))
                .isInstanceOf(NotPermittedException.class);
        for (int bad : new int[] {0, 201}) {
            assertThatThrownBy(() -> service.execute(actor("SUPER_ADMIN"), query(null, null, bad)))
                    .isInstanceOf(IllegalArgumentException.class);
        }
        assertThat(called).isEmpty();
    }
}
