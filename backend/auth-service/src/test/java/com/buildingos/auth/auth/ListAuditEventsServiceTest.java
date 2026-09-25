package com.buildingos.auth.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.auth.auth.application.PlatformRoleManagementNotPermittedException;
import com.buildingos.auth.auth.application.listauditevents.AuditEvent;
import com.buildingos.auth.auth.application.listauditevents.ListAuditEventsQuery;
import com.buildingos.auth.auth.application.listauditevents.ListAuditEventsService;
import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.model.PlatformRoleAuditAction;
import com.buildingos.auth.auth.domain.model.PlatformRoleAuditEntry;
import com.buildingos.auth.auth.domain.repository.PlatformRoleAuditRepository;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ListAuditEventsServiceTest {
    private static final Set<String> SUPER = Set.of("SUPER_ADMIN");
    private final UUID actor = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final List<Object[]> calls = new ArrayList<>();
    private final List<PlatformRoleAuditEntry> rows = List.of(
            new PlatformRoleAuditEntry(UUID.randomUUID(), actor, target, PlatformRole.SUPPORT_AGENT,
                    PlatformRoleAuditAction.REVOKE, Instant.parse("2026-09-25T10:00:00Z")),
            new PlatformRoleAuditEntry(UUID.randomUUID(), actor, target, PlatformRole.SUPPORT_AGENT,
                    PlatformRoleAuditAction.GRANT, Instant.parse("2026-09-25T09:00:00Z")));
    private final PlatformRoleAuditRepository audits = new PlatformRoleAuditRepository() {
        @Override public void append(PlatformRoleAuditEntry entry) { throw new UnsupportedOperationException(); }
        @Override public List<PlatformRoleAuditEntry> list(Instant since, Instant until, UUID actorUserId, int limit) {
            calls.add(new Object[] {since, until, actorUserId, limit});
            return rows;
        }
    };
    private final ListAuditEventsService service = new ListAuditEventsService(audits);

    private List<AuditEvent> run(Set<String> roles, String entityType, Integer limit) {
        return service.execute(new ListAuditEventsQuery(roles, null, null, entityType, null, limit));
    }

    @Test
    void mapsRowsToCommonItemsWithoutPayloads() {
        var events = run(SUPER, null, null);
        assertThat(events).hasSize(2);
        AuditEvent first = events.get(0);
        assertThat(first.source()).isEqualTo("auth-service");
        assertThat(first.action()).isEqualTo("PLATFORM_ROLE_REVOKED");
        assertThat(events.get(1).action()).isEqualTo("PLATFORM_ROLE_GRANTED");
        assertThat(first.entityType()).isEqualTo("PLATFORM_ROLE");
        assertThat(first.entityId()).isEqualTo(target);
        assertThat(first.actorUserId()).isEqualTo(actor);
        assertThat(first.buildingId()).isNull();
        assertThat(first.reason()).isNull();
        assertThat(first.role()).isEqualTo("SUPPORT_AGENT");
        assertThat(first.occurredAt()).isEqualTo(Instant.parse("2026-09-25T10:00:00Z"));
    }

    @Test
    void passesFiltersAndDefaultsLimitTo50() {
        Instant since = Instant.parse("2026-09-01T00:00:00Z");
        Instant until = Instant.parse("2026-10-01T00:00:00Z");
        service.execute(new ListAuditEventsQuery(SUPER, since, until, "PLATFORM_ROLE", actor, null));
        assertThat(calls).hasSize(1);
        assertThat(calls.get(0)).containsExactly(since, until, actor, 50);
        run(SUPER, null, 200);
        assertThat(calls.get(1)[3]).isEqualTo(200);
    }

    @Test
    void rejectsLimitOutOfRange() {
        assertThatThrownBy(() -> run(SUPER, null, 201)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> run(SUPER, null, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(calls).isEmpty();
    }

    @Test
    void otherEntityTypeYieldsEmptyListWithoutQuerying() {
        assertThat(run(SUPER, "BUILDING", null)).isEmpty();
        assertThat(calls).isEmpty();
    }

    @Test
    void onlySuperAdminMayRead() {
        for (Set<String> roles : List.of(Set.<String>of(), Set.of("PLATFORM_ADMIN"), Set.of("SUPPORT_AGENT"))) {
            assertThatThrownBy(() -> run(roles, null, null))
                    .isInstanceOf(PlatformRoleManagementNotPermittedException.class);
        }
        assertThat(calls).isEmpty();
    }
}
