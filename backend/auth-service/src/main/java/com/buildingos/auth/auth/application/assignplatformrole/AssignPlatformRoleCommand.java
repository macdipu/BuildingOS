package com.buildingos.auth.auth.application.assignplatformrole;

import com.buildingos.auth.auth.domain.model.PlatformRole;
import java.util.Set;
import java.util.UUID;

/** {@code callerUserId}/{@code callerPlatformRoles} come from the caller's validated access token. */
public record AssignPlatformRoleCommand(
        UUID callerUserId, Set<String> callerPlatformRoles, UUID targetUserId, PlatformRole role) {
    public AssignPlatformRoleCommand {
        callerPlatformRoles = Set.copyOf(callerPlatformRoles);
    }
}
