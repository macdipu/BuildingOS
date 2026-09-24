package com.buildingos.auth.auth.application.provisionuser;

import java.util.Set;

/** {@code callerPlatformRoles} come from the caller's validated access token. */
public record ProvisionUserCommand(Set<String> callerPlatformRoles, String phone) {
    public ProvisionUserCommand {
        callerPlatformRoles = Set.copyOf(callerPlatformRoles);
    }
}
