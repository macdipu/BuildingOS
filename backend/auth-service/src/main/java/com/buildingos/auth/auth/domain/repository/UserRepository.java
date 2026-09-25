package com.buildingos.auth.auth.domain.repository;

import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.model.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findByPhone(String phone);

    Optional<User> findById(UUID id);

    /** Finds the user by phone, creating one with no platform roles if none exists yet. */
    User findOrCreateByPhone(String phone);

    /**
     * Users whose phone contains {@code query} (case-insensitive, ignored if null/blank) and who hold
     * {@code role} (ignored if null), newest first, paginated.
     */
    List<User> list(String query, PlatformRole role, int page, int size);

    /** Total matching {@link #list} for the same filters, ignoring page/size. */
    long count(String query, PlatformRole role);

    /** Grants the platform role to the user; a no-op (returns false) if the user already holds it. */
    boolean grantPlatformRole(UUID userId, PlatformRole role);

    /** Revokes the platform role from the user; a no-op (returns false) if the user does not hold it. */
    boolean revokePlatformRole(UUID userId, PlatformRole role);
}
