package com.buildingos.auth.auth.application.revokeplatformrole;

import com.buildingos.auth.auth.domain.model.PlatformRole;
import java.util.Set;
import java.util.UUID;

/** {@code callerUserId}/{@code callerPlatformRoles} come from the caller's validated access token. */
public record RevokePlatformRoleCommand(
        UUID callerUserId, Set<String> callerPlatformRoles, UUID targetUserId, PlatformRole role) {
    public RevokePlatformRoleCommand {
        callerPlatformRoles = Set.copyOf(callerPlatformRoles);
    }
}
