package com.buildingos.building.shared.application;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Authenticated caller: user id and platform roles from the access token. */
public record Actor(UUID userId, Set<String> platformRoles) {
    /** Platform roles allowed to review applications and run building lifecycle actions (D-07, BA-05). */
    public static final Set<String> PLATFORM_ADMIN_ROLES = Set.of("SUPER_ADMIN", "PLATFORM_ADMIN");

    public Actor {
        Objects.requireNonNull(userId, "userId");
        platformRoles = Set.copyOf(platformRoles);
    }

    public boolean isPlatformAdmin() {
        return platformRoles.stream().anyMatch(PLATFORM_ADMIN_ROLES::contains);
    }

    public void requirePlatformAdmin() {
        if (!isPlatformAdmin()) {
            throw new NotPermittedException();
        }
    }
}
