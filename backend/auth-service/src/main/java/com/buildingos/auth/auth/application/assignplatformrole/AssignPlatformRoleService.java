package com.buildingos.auth.auth.application.assignplatformrole;

import com.buildingos.auth.auth.application.PlatformRoleManagementNotPermittedException;
import com.buildingos.auth.auth.application.UserNotFoundException;
import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.model.User;
import com.buildingos.auth.auth.domain.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SUPER_ADMIN only (D-10); no caller may grant themselves a platform role (extends ID-04). */
public final class AssignPlatformRoleService implements AssignPlatformRoleUseCase {
    private static final Logger log = LoggerFactory.getLogger(AssignPlatformRoleService.class);
    private final UserRepository users;

    public AssignPlatformRoleService(UserRepository users) {
        this.users = users;
    }

    @Override
    public User execute(AssignPlatformRoleCommand command) {
        if (!command.callerPlatformRoles().contains(PlatformRole.SUPER_ADMIN.name())) {
            throw new PlatformRoleManagementNotPermittedException();
        }
        if (command.callerUserId().equals(command.targetUserId())) {
            throw new PlatformRoleManagementNotPermittedException();
        }
        if (users.findById(command.targetUserId()).isEmpty()) {
            throw new UserNotFoundException();
        }
        users.grantPlatformRole(command.targetUserId(), command.role());
        log.info("platform_role_granted actor={} target={} role={}",
                command.callerUserId(), command.targetUserId(), command.role());
        return users.findById(command.targetUserId()).orElseThrow(UserNotFoundException::new);
    }
}
