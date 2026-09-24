package com.buildingos.auth.auth.application.provisionuser;

import com.buildingos.auth.auth.domain.model.PhoneNumber;
import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.repository.UserRepository;
import java.util.Set;

/**
 * Used by building-service when an approver names the initial building admin by phone (D-12, D-29). The person
 * may never have logged in; they sign in later with the normal OTP flow and get the same user id.
 */
public final class ProvisionUserService implements ProvisionUserUseCase {
    private static final Set<String> ALLOWED = Set.of(PlatformRole.SUPER_ADMIN.name(), PlatformRole.PLATFORM_ADMIN.name());
    private final UserRepository users;

    public ProvisionUserService(UserRepository users) {
        this.users = users;
    }

    @Override
    public ProvisionUserResult execute(ProvisionUserCommand command) {
        if (command.callerPlatformRoles().stream().noneMatch(ALLOWED::contains)) {
            throw new ProvisioningNotPermittedException();
        }
        var user = users.findOrCreateByPhone(PhoneNumber.parse(command.phone()).value());
        return new ProvisionUserResult(user.id(), user.phone());
    }
}
