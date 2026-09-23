package com.buildingos.account.auth.application.seedsuperadmin;

import com.buildingos.account.auth.domain.model.PlatformRole;
import com.buildingos.account.auth.domain.repository.UserRepository;

public final class SeedSuperAdminService implements SeedSuperAdminUseCase {
    private final UserRepository users;

    public SeedSuperAdminService(UserRepository users) {
        this.users = users;
    }

    @Override
    public void execute(SeedSuperAdminCommand command) {
        var user = users.findOrCreateByPhone(command.phone());
        users.grantPlatformRole(user.id(), PlatformRole.SUPER_ADMIN);
    }
}
