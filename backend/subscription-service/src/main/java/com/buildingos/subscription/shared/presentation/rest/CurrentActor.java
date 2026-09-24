package com.buildingos.subscription.shared.presentation.rest;

import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.shared.application.NotPermittedException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.security.oauth2.jwt.Jwt;

/** Builds the caller from auth-service's access token: {@code sub} = user id, {@code platform_roles} claim. */
public final class CurrentActor {
    private CurrentActor() {}

    public static Actor from(Jwt jwt) {
        UUID userId;
        try {
            userId = UUID.fromString(jwt.getSubject());
        } catch (IllegalArgumentException | NullPointerException notAUser) {
            throw new NotPermittedException();
        }
        List<String> roles = jwt.getClaimAsStringList("platform_roles");
        return new Actor(userId, roles == null ? Set.of() : Set.copyOf(roles));
    }
}
