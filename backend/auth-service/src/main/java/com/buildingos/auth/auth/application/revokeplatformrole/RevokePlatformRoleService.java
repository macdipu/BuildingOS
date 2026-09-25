package com.buildingos.auth.auth.application.revokeplatformrole;

import com.buildingos.auth.auth.application.PlatformRoleManagementNotPermittedException;
import com.buildingos.auth.auth.application.UserNotFoundException;
import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SUPER_ADMIN only (D-10); no caller may revoke their own platform role (extends ID-04). */
public final class RevokePlatformRoleService implements RevokePlatformRoleUseCase {
    private static final Logger log = LoggerFactory.getLogger(RevokePlatformRoleService.class);
    private final UserRepository users;

    public RevokePlatformRoleService(UserRepository users) {
        this.users = users;
    }

    @Override
    public void execute(RevokePlatformRoleCommand command) {
        if (!command.callerPlatformRoles().contains(PlatformRole.SUPER_ADMIN.name())) {
            throw new PlatformRoleManagementNotPermittedException();
        }
        if (command.callerUserId().equals(command.targetUserId())) {
            throw new PlatformRoleManagementNotPermittedException();
        }
        if (users.findById(command.targetUserId()).isEmpty()) {
            throw new UserNotFoundException();
        }
        users.revokePlatformRole(command.targetUserId(), command.role());
        log.info("platform_role_revoked actor={} target={} role={}",
                command.callerUserId(), command.targetUserId(), command.role());
    }
}
