package com.buildingos.backoffice.platformuser.domain.model;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** A user as auth-service reports it: identity, phone and current platform roles. auth-service owns the data. */
public record PlatformUser(UUID id, String phone, Instant createdAt, List<String> platformRoles) {
    public PlatformUser {
        platformRoles = List.copyOf(platformRoles);
    }
}
