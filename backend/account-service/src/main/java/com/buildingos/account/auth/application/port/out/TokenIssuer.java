package com.buildingos.account.auth.application.port.out;

import com.buildingos.account.auth.domain.model.PlatformRole;
import java.util.Set;
import java.util.UUID;

public interface TokenIssuer {
    IssuedToken issue(UUID userId, String phone, Set<PlatformRole> platformRoles);

    record IssuedToken(String accessToken, long expiresInSeconds) {}
}
