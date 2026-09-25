package com.buildingos.backoffice.shared.application;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Authenticated caller: user id and platform roles from the access token. */
public record Actor(UUID userId, Set<String> platformRoles) {
    /** Platform operators (D-07, D-33a): create/assign, complete and cancel sessions. */
    public static final Set<String> OPERATOR_ROLES = Set.of("SUPER_ADMIN", "PLATFORM_ADMIN");
    public static final String ONBOARDING_AGENT = "ONBOARDING_AGENT";
    public static final String SUPPORT_AGENT = "SUPPORT_AGENT";
    public static final String SUPER_ADMIN = "SUPER_ADMIN";

    public Actor {
        Objects.requireNonNull(userId, "userId");
        platformRoles = Set.copyOf(platformRoles);
    }

    public boolean isOperator() {
        return platformRoles.stream().anyMatch(OPERATOR_ROLES::contains);
    }

    public boolean isOnboardingAgent() {
        return platformRoles.contains(ONBOARDING_AGENT);
    }

    public boolean isSupportAgent() {
        return platformRoles.contains(SUPPORT_AGENT);
    }

    public boolean isSuperAdmin() {
        return platformRoles.contains(SUPER_ADMIN);
    }

    public void requireOperator() {
        if (!isOperator()) {
            throw new NotPermittedException();
        }
    }
}
