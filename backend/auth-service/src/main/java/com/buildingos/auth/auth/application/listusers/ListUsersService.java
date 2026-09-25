package com.buildingos.auth.auth.application.listusers;

import com.buildingos.auth.auth.application.Page;
import com.buildingos.auth.auth.application.PlatformRoleManagementNotPermittedException;
import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.model.User;
import com.buildingos.auth.auth.domain.repository.UserRepository;
import java.util.Set;

public final class ListUsersService implements ListUsersUseCase {
    private static final Set<String> ALLOWED = Set.of(PlatformRole.SUPER_ADMIN.name(), PlatformRole.PLATFORM_ADMIN.name());
    private final UserRepository users;

    public ListUsersService(UserRepository users) {
        this.users = users;
    }

    @Override
    public Page<User> execute(ListUsersQuery query) {
        if (query.callerPlatformRoles().stream().noneMatch(ALLOWED::contains)) {
            throw new PlatformRoleManagementNotPermittedException();
        }
        Page.validate(query.page(), query.size());
        var items = users.list(query.query(), query.role(), query.page(), query.size());
        var total = users.count(query.query(), query.role());
        return new Page<>(items, query.page(), query.size(), total);
    }
}
