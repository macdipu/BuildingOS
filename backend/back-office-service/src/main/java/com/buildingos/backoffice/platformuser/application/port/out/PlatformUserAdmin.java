package com.buildingos.backoffice.platformuser.application.port.out;

import com.buildingos.backoffice.platformuser.domain.model.PlatformUser;
import com.buildingos.backoffice.shared.application.Page;
import java.util.Optional;
import java.util.UUID;

/**
 * auth-service platform user management with the caller's relayed token. auth-service authorizes every call
 * (including that nobody changes their own roles). Throws {@code NotPermittedException} on 403 and
 * {@code DependencyUnavailableException} when auth-service cannot answer; an unknown user is an empty Optional.
 */
public interface PlatformUserAdmin {
    Page<PlatformUser> list(String query, String role, int page, int size);

    Optional<PlatformUser> find(UUID userId);

    /** Idempotent. Returns the user after the change. */
    Optional<PlatformUser> assign(UUID userId, String role);

    /** Idempotent. Returns the user after the change. */
    Optional<PlatformUser> revoke(UUID userId, String role);
}
