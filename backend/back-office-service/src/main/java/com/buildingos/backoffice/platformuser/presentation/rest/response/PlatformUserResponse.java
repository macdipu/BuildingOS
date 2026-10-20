package com.buildingos.backoffice.platformuser.presentation.rest.response;

import com.buildingos.backoffice.platformuser.domain.model.PlatformUser;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlatformUserResponse(UUID id, String phone, Instant createdAt, List<String> platformRoles) {
    public static PlatformUserResponse of(PlatformUser user) {
        return new PlatformUserResponse(user.id(), user.phone(), user.createdAt(), user.platformRoles());
    }
}
