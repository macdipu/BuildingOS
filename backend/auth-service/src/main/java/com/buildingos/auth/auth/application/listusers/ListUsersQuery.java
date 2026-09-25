package com.buildingos.auth.auth.application.listusers;

import com.buildingos.auth.auth.domain.model.PlatformRole;
import java.util.Set;

/** {@code callerPlatformRoles} come from the caller's validated access token. */
public record ListUsersQuery(Set<String> callerPlatformRoles, String query, PlatformRole role, int page, int size) {
    public ListUsersQuery {
        callerPlatformRoles = Set.copyOf(callerPlatformRoles);
    }
}
