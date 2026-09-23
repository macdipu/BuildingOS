package com.buildingos.account.auth.domain.repository;

import com.buildingos.account.auth.domain.model.PlatformRole;
import com.buildingos.account.auth.domain.model.User;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    Optional<User> findByPhone(String phone);

    /** Finds the user by phone, creating one with no platform roles if none exists yet. */
    User findOrCreateByPhone(String phone);

    /** Grants the platform role to the user; a no-op if the user already holds it. */
    void grantPlatformRole(UUID userId, PlatformRole role);
}
