package com.buildingos.auth.auth.application.getdirectoryuser;

import com.buildingos.auth.auth.application.PlatformRoleManagementNotPermittedException;
import com.buildingos.auth.auth.application.UserNotFoundException;
import com.buildingos.auth.auth.domain.model.User;
import com.buildingos.auth.auth.domain.repository.UserRepository;

/** Existence + platform roles of one user for service callers holding {@code directory.read} (D-38). */
public final class GetDirectoryUserService implements GetDirectoryUserUseCase {
    public static final String DIRECTORY_READ = "directory.read";
    private final UserRepository users;

    public GetDirectoryUserService(UserRepository users) {
        this.users = users;
    }

    @Override
    public User execute(GetDirectoryUserQuery query) {
        if (!query.callerServiceScopes().contains(DIRECTORY_READ)) {
            throw new PlatformRoleManagementNotPermittedException();
        }
        return users.findById(query.userId()).orElseThrow(UserNotFoundException::new);
    }
}
