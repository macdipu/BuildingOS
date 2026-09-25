package com.buildingos.auth.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.auth.auth.application.UserNotFoundException;
import com.buildingos.auth.auth.application.assignplatformrole.AssignPlatformRoleCommand;
import com.buildingos.auth.auth.application.assignplatformrole.AssignPlatformRoleService;
import com.buildingos.auth.auth.application.port.out.UnitOfWork;
import com.buildingos.auth.auth.application.revokeplatformrole.RevokePlatformRoleCommand;
import com.buildingos.auth.auth.application.revokeplatformrole.RevokePlatformRoleService;
import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.model.PlatformRoleAuditAction;
import com.buildingos.auth.auth.domain.model.PlatformRoleAuditEntry;
import com.buildingos.auth.auth.domain.model.User;
import com.buildingos.auth.auth.domain.repository.PlatformRoleAuditRepository;
import com.buildingos.auth.auth.domain.repository.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** D-37: an effective grant/revoke appends one audit row inside the unit of work; no-ops append nothing. */
class PlatformRoleAuditWriteTest {
    private static final Instant NOW = Instant.parse("2026-09-25T12:00:00Z");
    private static final Set<String> SUPER = Set.of("SUPER_ADMIN");
    private final UUID actor = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();
    private final Map<UUID, Set<PlatformRole>> roles = new HashMap<>(Map.of(target, EnumSet.noneOf(PlatformRole.class)));
    private final List<PlatformRoleAuditEntry> appended = new ArrayList<>();
    private boolean inTransaction;
    private int transactions;

    private final UserRepository users = new UserRepository() {
        @Override public Optional<User> findById(UUID id) {
            return Optional.ofNullable(roles.get(id)).map(r -> new User(id, "01755600099", Instant.EPOCH, Set.copyOf(r)));
        }
        @Override public boolean grantPlatformRole(UUID userId, PlatformRole role) { return roles.get(userId).add(role); }
        @Override public boolean revokePlatformRole(UUID userId, PlatformRole role) { return roles.get(userId).remove(role); }
        @Override public Optional<User> findByPhone(String phone) { throw new UnsupportedOperationException(); }
        @Override public User findOrCreateByPhone(String phone) { throw new UnsupportedOperationException(); }
        @Override public List<User> list(String query, PlatformRole role, int page, int size) {
            throw new UnsupportedOperationException();
        }
        @Override public long count(String query, PlatformRole role) { throw new UnsupportedOperationException(); }
    };
    private final PlatformRoleAuditRepository audits = new PlatformRoleAuditRepository() {
        @Override public void append(PlatformRoleAuditEntry entry) {
            assertThat(inTransaction).as("audit row written inside the role-change transaction").isTrue();
            appended.add(entry);
        }
        @Override public List<PlatformRoleAuditEntry> list(Instant since, Instant until, UUID actorUserId, int limit) {
            throw new UnsupportedOperationException();
        }
    };
    private final UnitOfWork uow = new UnitOfWork() {
        @Override public <T> T inTransaction(java.util.function.Supplier<T> work) {
            transactions++;
            inTransaction = true;
            try {
                return work.get();
            } finally {
                inTransaction = false;
            }
        }
    };
    private final Clock clock = Clock.fixed(NOW, ZoneOffset.UTC);
    private final AssignPlatformRoleService assign = new AssignPlatformRoleService(users, audits, uow, clock);
    private final RevokePlatformRoleService revoke = new RevokePlatformRoleService(users, audits, uow, clock);

    @Test
    void effectiveGrantAndRevokeEachAppendOneRow() {
        assign.execute(new AssignPlatformRoleCommand(actor, SUPER, target, PlatformRole.SUPPORT_AGENT));
        revoke.execute(new RevokePlatformRoleCommand(actor, SUPER, target, PlatformRole.SUPPORT_AGENT));
        assertThat(appended).extracting(PlatformRoleAuditEntry::action)
                .containsExactly(PlatformRoleAuditAction.GRANT, PlatformRoleAuditAction.REVOKE);
        assertThat(appended).allSatisfy(entry -> {
            assertThat(entry.actorUserId()).isEqualTo(actor);
            assertThat(entry.targetUserId()).isEqualTo(target);
            assertThat(entry.role()).isEqualTo(PlatformRole.SUPPORT_AGENT);
            assertThat(entry.occurredAt()).isEqualTo(NOW);
        });
        assertThat(transactions).isEqualTo(2);
    }

    @Test
    void noOpGrantAndRevokeAppendNothing() {
        assign.execute(new AssignPlatformRoleCommand(actor, SUPER, target, PlatformRole.ONBOARDING_AGENT));
        assign.execute(new AssignPlatformRoleCommand(actor, SUPER, target, PlatformRole.ONBOARDING_AGENT));
        revoke.execute(new RevokePlatformRoleCommand(actor, SUPER, target, PlatformRole.SUPPORT_AGENT));
        assertThat(appended).hasSize(1);
        assertThat(appended.get(0).action()).isEqualTo(PlatformRoleAuditAction.GRANT);
    }

    @Test
    void unknownUserAppendsNothing() {
        UUID missing = UUID.randomUUID();
        assertThatThrownBy(() -> assign.execute(
                new AssignPlatformRoleCommand(actor, SUPER, missing, PlatformRole.SUPPORT_AGENT)))
                .isInstanceOf(UserNotFoundException.class);
        assertThatThrownBy(() -> revoke.execute(
                new RevokePlatformRoleCommand(actor, SUPER, missing, PlatformRole.SUPPORT_AGENT)))
                .isInstanceOf(UserNotFoundException.class);
        assertThat(appended).isEmpty();
    }
}
