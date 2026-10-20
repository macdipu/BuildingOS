package com.buildingos.backoffice.platformuser.domain.model;

import java.util.Set;

/** Platform role names (auth-service {@code PlatformRole}); used only to reject a typo before a remote call. */
public final class PlatformRoles {
    public static final Set<String> ALL = Set.of(
            "SUPER_ADMIN", "PLATFORM_ADMIN", "ONBOARDING_AGENT", "SUPPORT_AGENT", "SUBSCRIPTION_ADMIN");

    private PlatformRoles() {}

    public static String require(String role) {
        if (role == null || !ALL.contains(role)) {
            throw new IllegalArgumentException("Unknown platform role: " + role);
        }
        return role;
    }
}
