package com.buildingos.auth.auth.presentation.rest.response;

import com.buildingos.auth.auth.domain.model.User;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record PlatformUserResponse(UUID id, String phone, Instant createdAt, List<String> platformRoles) {
    public static PlatformUserResponse of(User user) {
        return new PlatformUserResponse(user.id(), user.phone(), user.createdAt(),
                user.platformRoles().stream().map(Enum::name).sorted().toList());
    }
}
