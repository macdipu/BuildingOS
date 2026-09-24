package com.buildingos.auth.auth.domain.model;

/** Platform-scoped roles (BRD §4.1). Independent of any building membership/role. */
public enum PlatformRole {
    SUPER_ADMIN,
    PLATFORM_ADMIN,
    ONBOARDING_AGENT,
    SUPPORT_AGENT,
    SUBSCRIPTION_ADMIN
}
