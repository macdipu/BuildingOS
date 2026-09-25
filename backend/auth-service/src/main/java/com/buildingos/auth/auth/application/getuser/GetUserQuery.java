package com.buildingos.auth.auth.application.getuser;

import java.util.Set;
import java.util.UUID;

public record GetUserQuery(Set<String> callerPlatformRoles, UUID userId) {
    public GetUserQuery {
        callerPlatformRoles = Set.copyOf(callerPlatformRoles);
    }
}
