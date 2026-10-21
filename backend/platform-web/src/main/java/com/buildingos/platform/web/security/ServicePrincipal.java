package com.buildingos.platform.web.security;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Service-to-service callers (BOS-010 D-38): auth-service issues them a token whose {@code sub} is
 * {@code service:<client id>} and whose {@code scope} names what they may read. User tokens never qualify.
 */
public final class ServicePrincipal {
    public static final String SUBJECT_PREFIX = "service:";

    private ServicePrincipal() {}

    /** Scopes granted to a service caller; empty for a user token or a token without {@code scope}. */
    public static Set<String> scopes(Jwt jwt) {
        String subject = jwt.getSubject();
        String scope = jwt.getClaimAsString("scope");
        if (subject == null || !subject.startsWith(SUBJECT_PREFIX) || scope == null || scope.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(scope.trim().split("\\s+")).collect(Collectors.toUnmodifiableSet());
    }
}
