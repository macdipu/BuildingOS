package com.buildingos.subscription.shared.application;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/** Authenticated caller: user id and platform roles from the access token. */
public record Actor(UUID userId, Set<String> platformRoles) {
    /** Platform roles allowed to manage plans, subscriptions, free tier and fees (D-08). */
    public static final Set<String> REVENUE_ADMIN_ROLES = Set.of("SUPER_ADMIN", "PLATFORM_ADMIN", "SUBSCRIPTION_ADMIN");

    public Actor {
        Objects.requireNonNull(userId, "userId");
        platformRoles = Set.copyOf(platformRoles);
    }

    public void requireRevenueAdmin() {
        if (platformRoles.stream().noneMatch(REVENUE_ADMIN_ROLES::contains)) {
            throw new NotPermittedException();
        }
    }
}
