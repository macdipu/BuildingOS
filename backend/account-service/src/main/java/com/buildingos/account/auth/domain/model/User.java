package com.buildingos.account.auth.domain.model;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

/** Global BuildingOS user identity (BRD §110.1). Never duplicated per building. */
public record User(UUID id, String phone, Instant createdAt, Set<PlatformRole> platformRoles) {
    public User {
        platformRoles = Set.copyOf(platformRoles);
    }
}
