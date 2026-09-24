package com.buildingos.auth.auth;

import com.buildingos.auth.auth.application.seedsuperadmin.SeedSuperAdminCommand;
import com.buildingos.auth.auth.application.seedsuperadmin.SeedSuperAdminService;
import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.model.User;
import com.buildingos.auth.auth.domain.repository.UserRepository;
import java.time.Instant;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class SeedSuperAdminServiceTest {
    private static final String PHONE = "01306999005";

    private final Map<String, UUID> ids = new HashMap<>();
    private final Map<UUID, Set<PlatformRole>> roles = new HashMap<>();
    private final UserRepository users = new UserRepository() {
        @Override public Optional<User> findByPhone(String phone) {
            return Optional.ofNullable(ids.get(phone))
                    .map(id -> new User(id, phone, Instant.EPOCH, roles.getOrDefault(id, Set.of())));
        }
        @Override public User findOrCreateByPhone(String phone) {
            ids.computeIfAbsent(phone, p -> UUID.randomUUID());
            return findByPhone(phone).orElseThrow();
        }
        @Override public void grantPlatformRole(UUID userId, PlatformRole role) {
            roles.computeIfAbsent(userId, id -> EnumSet.noneOf(PlatformRole.class)).add(role);
        }
    };

    private final SeedSuperAdminService service = new SeedSuperAdminService(users);

    @Test
    void grantsSuperAdminIdempotently() {
        service.execute(new SeedSuperAdminCommand(PHONE));
        service.execute(new SeedSuperAdminCommand(PHONE));
        assertThat(ids).hasSize(1);
        assertThat(users.findByPhone(PHONE).orElseThrow().platformRoles()).containsExactly(PlatformRole.SUPER_ADMIN);
    }
}
