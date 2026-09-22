package com.buildingos.identity.auth.application.port.out;

import com.buildingos.identity.auth.domain.PlatformRole;
import java.util.Set;
import java.util.UUID;

public interface TokenIssuer {
    IssuedToken issue(UUID userId, String phone, Set<PlatformRole> platformRoles);

    record IssuedToken(String accessToken, long expiresInSeconds) {}
}
