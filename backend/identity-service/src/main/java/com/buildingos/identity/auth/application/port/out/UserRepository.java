package com.buildingos.identity.auth.application.port.out;

import com.buildingos.identity.auth.domain.User;
import java.util.Optional;

public interface UserRepository {
    Optional<User> findByPhone(String phone);

    /** Finds the user by phone, creating one with no platform roles if none exists yet. */
    User findOrCreateByPhone(String phone);
}
