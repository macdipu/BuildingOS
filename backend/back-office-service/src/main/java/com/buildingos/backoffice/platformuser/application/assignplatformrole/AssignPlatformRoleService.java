package com.buildingos.backoffice.platformuser.application.assignplatformrole;

import com.buildingos.backoffice.platformuser.application.port.out.PlatformUserAdmin;
import com.buildingos.backoffice.platformuser.domain.model.PlatformRoles;
import com.buildingos.backoffice.platformuser.domain.model.PlatformUser;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.BusinessRuleException;
import com.buildingos.backoffice.shared.application.NotPermittedException;

/**
 * SUPER_ADMIN only (BOC-05). auth-service re-checks the role and refuses a user changing their own roles; the
 * change and its audit row are written there (D-37), so this service stores nothing.
 */
public final class AssignPlatformRoleService implements AssignPlatformRoleUseCase {
    private final PlatformUserAdmin users;

    public AssignPlatformRoleService(PlatformUserAdmin users) { this.users = users; }

    @Override
    public PlatformUser execute(Actor actor, AssignPlatformRoleCommand command) {
        if (!actor.isSuperAdmin()) {
            throw new NotPermittedException();
        }
        String role = PlatformRoles.require(command.role());
        return users.assign(command.userId(), role)
                .orElseThrow(() -> BusinessRuleException.notFound("USER_NOT_FOUND", "User not found"));
    }
}
