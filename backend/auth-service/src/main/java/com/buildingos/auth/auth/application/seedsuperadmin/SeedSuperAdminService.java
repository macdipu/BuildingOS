package com.buildingos.auth.auth.application.seedsuperadmin;

import com.buildingos.auth.auth.domain.model.PhoneNumber;
import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.repository.UserRepository;

public final class SeedSuperAdminService implements SeedSuperAdminUseCase {
    private final UserRepository users;

    public SeedSuperAdminService(UserRepository users) {
        this.users = users;
    }

    @Override
    public void execute(SeedSuperAdminCommand command) {
        var user = users.findOrCreateByPhone(PhoneNumber.parse(command.phone()).value());
        users.grantPlatformRole(user.id(), PlatformRole.SUPER_ADMIN);
    }
}
