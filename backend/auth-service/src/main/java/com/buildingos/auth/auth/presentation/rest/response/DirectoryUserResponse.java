package com.buildingos.auth.auth.presentation.rest.response;

import com.buildingos.auth.auth.domain.model.User;
import java.util.List;
import java.util.UUID;

/** Directory view for service callers: no phone or other personal data (D-38). */
public record DirectoryUserResponse(UUID id, List<String> platformRoles) {
    public static DirectoryUserResponse of(User user) {
        return new DirectoryUserResponse(user.id(), user.platformRoles().stream().map(Enum::name).sorted().toList());
    }
}
