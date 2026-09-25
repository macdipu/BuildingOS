package com.buildingos.backoffice.onboarding.application.port.out;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/** auth-service user directory. Throws {@code DependencyUnavailableException} when auth-service cannot answer. */
public interface PlatformUserDirectory {
    /** The user's current platform roles; empty when the user does not exist. */
    Optional<Set<String>> platformRolesOf(UUID userId);
}
