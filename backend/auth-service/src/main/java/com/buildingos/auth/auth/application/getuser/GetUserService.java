package com.buildingos.auth.auth.application.getuser;

import com.buildingos.auth.auth.application.PlatformRoleManagementNotPermittedException;
import com.buildingos.auth.auth.application.UserNotFoundException;
import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.model.User;
import com.buildingos.auth.auth.domain.repository.UserRepository;
import java.util.Set;

public final class GetUserService implements GetUserUseCase {
    private static final Set<String> ALLOWED = Set.of(PlatformRole.SUPER_ADMIN.name(), PlatformRole.PLATFORM_ADMIN.name());
    private final UserRepository users;

    public GetUserService(UserRepository users) {
        this.users = users;
    }

    @Override
    public User execute(GetUserQuery query) {
        if (query.callerPlatformRoles().stream().noneMatch(ALLOWED::contains)) {
            throw new PlatformRoleManagementNotPermittedException();
        }
        return users.findById(query.userId()).orElseThrow(UserNotFoundException::new);
    }
}
