package com.buildingos.backoffice.platformuser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.backoffice.platformuser.application.assignplatformrole.AssignPlatformRoleCommand;
import com.buildingos.backoffice.platformuser.application.assignplatformrole.AssignPlatformRoleService;
import com.buildingos.backoffice.platformuser.application.getplatformuser.GetPlatformUserService;
import com.buildingos.backoffice.platformuser.application.listplatformusers.ListPlatformUsersQuery;
import com.buildingos.backoffice.platformuser.application.listplatformusers.ListPlatformUsersService;
import com.buildingos.backoffice.platformuser.application.port.out.PlatformUserAdmin;
import com.buildingos.backoffice.platformuser.application.revokeplatformrole.RevokePlatformRoleCommand;
import com.buildingos.backoffice.platformuser.application.revokeplatformrole.RevokePlatformRoleService;
import com.buildingos.backoffice.platformuser.domain.model.PlatformUser;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.BusinessRuleException;
import com.buildingos.backoffice.shared.application.NotPermittedException;
import com.buildingos.backoffice.shared.application.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class PlatformUserUseCaseTest {
    private final List<String> calls = new ArrayList<>();
    private boolean known = true;
    private final PlatformUserAdmin remote = new PlatformUserAdmin() {
        private PlatformUser user(UUID id) { return new PlatformUser(id, "017", Instant.EPOCH, List.of("SUPPORT_AGENT")); }

        public Page<PlatformUser> list(String q, String role, int page, int size) {
            calls.add("list:" + q + ":" + role);
            return new Page<>(List.of(), page, size, 0);
        }
        public Optional<PlatformUser> find(UUID id) { calls.add("find"); return known ? Optional.of(user(id)) : Optional.empty(); }
        public Optional<PlatformUser> assign(UUID id, String role) { calls.add("assign:" + role); return known ? Optional.of(user(id)) : Optional.empty(); }
        public Optional<PlatformUser> revoke(UUID id, String role) { calls.add("revoke:" + role); return known ? Optional.of(user(id)) : Optional.empty(); }
    };
    private final UUID target = UUID.randomUUID();
    private static Actor actor(String... roles) { return new Actor(UUID.randomUUID(), Set.of(roles)); }

    @Test
    void readsAreForOperatorsOnly() {
        var list = new ListPlatformUsersService(remote);
        var get = new GetPlatformUserService(remote);
        for (String role : List.of("SUPER_ADMIN", "PLATFORM_ADMIN")) {
            list.execute(actor(role), new ListPlatformUsersQuery(" 017 ", "SUPPORT_AGENT", 0, 20));
            get.execute(actor(role), target);
        }
        assertThat(calls).contains("list:017:SUPPORT_AGENT");
        for (String role : List.of("SUPPORT_AGENT", "ONBOARDING_AGENT", "SUBSCRIPTION_ADMIN")) {
            assertThatThrownBy(() -> list.execute(actor(role), new ListPlatformUsersQuery(null, null, 0, 20)))
                    .isInstanceOf(NotPermittedException.class);
            assertThatThrownBy(() -> get.execute(actor(role), target)).isInstanceOf(NotPermittedException.class);
        }
    }

    @Test
    void listRejectsUnknownRoleAndBadPaging() {
        var list = new ListPlatformUsersService(remote);
        assertThatThrownBy(() -> list.execute(actor("SUPER_ADMIN"), new ListPlatformUsersQuery(null, "ROOT", 0, 20)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> list.execute(actor("SUPER_ADMIN"), new ListPlatformUsersQuery(null, null, 0, 101)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void roleChangesAreSuperAdminOnlyAndValidateTheRoleBeforeCalling() {
        var assign = new AssignPlatformRoleService(remote);
        var revoke = new RevokePlatformRoleService(remote);
        assertThatThrownBy(() -> assign.execute(actor("PLATFORM_ADMIN"), new AssignPlatformRoleCommand(target, "SUPPORT_AGENT")))
                .isInstanceOf(NotPermittedException.class);
        assertThatThrownBy(() -> revoke.execute(actor("PLATFORM_ADMIN"), new RevokePlatformRoleCommand(target, "SUPPORT_AGENT")))
                .isInstanceOf(NotPermittedException.class);
        assertThatThrownBy(() -> assign.execute(actor("SUPER_ADMIN"), new AssignPlatformRoleCommand(target, "ROOT")))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(calls).isEmpty();

        assign.execute(actor("SUPER_ADMIN"), new AssignPlatformRoleCommand(target, "SUPPORT_AGENT"));
        revoke.execute(actor("SUPER_ADMIN"), new RevokePlatformRoleCommand(target, "SUPPORT_AGENT"));
        assertThat(calls).containsExactly("assign:SUPPORT_AGENT", "revoke:SUPPORT_AGENT");
    }

    @Test
    void unknownUserIsUserNotFound() {
        known = false;
        assertThatThrownBy(() -> new GetPlatformUserService(remote).execute(actor("SUPER_ADMIN"), target))
                .isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.code()).isEqualTo("USER_NOT_FOUND"));
        assertThatThrownBy(() -> new AssignPlatformRoleService(remote)
                .execute(actor("SUPER_ADMIN"), new AssignPlatformRoleCommand(target, "SUPPORT_AGENT")))
                .isInstanceOfSatisfying(BusinessRuleException.class, e -> assertThat(e.code()).isEqualTo("USER_NOT_FOUND"));
    }
}
