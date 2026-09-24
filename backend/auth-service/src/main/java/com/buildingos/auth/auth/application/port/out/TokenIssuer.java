package com.buildingos.auth.auth.application.port.out;

import com.buildingos.auth.auth.domain.model.PlatformRole;
import java.util.Set;
import java.util.UUID;

public interface TokenIssuer {
    IssuedToken issue(UUID userId, String phone, Set<PlatformRole> platformRoles);

    record IssuedToken(String accessToken, long expiresInSeconds) {}
}
