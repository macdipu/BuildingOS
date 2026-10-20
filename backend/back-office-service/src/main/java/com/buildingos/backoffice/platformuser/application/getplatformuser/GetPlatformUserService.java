package com.buildingos.backoffice.platformuser.application.getplatformuser;

import com.buildingos.backoffice.platformuser.application.port.out.PlatformUserAdmin;
import com.buildingos.backoffice.platformuser.domain.model.PlatformUser;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.BusinessRuleException;
import java.util.UUID;

/** SUPER_ADMIN or PLATFORM_ADMIN (BOC-05). */
public final class GetPlatformUserService implements GetPlatformUserUseCase {
    private final PlatformUserAdmin users;

    public GetPlatformUserService(PlatformUserAdmin users) { this.users = users; }

    @Override
    public PlatformUser execute(Actor actor, UUID userId) {
        actor.requireOperator();
        return users.find(userId).orElseThrow(() -> BusinessRuleException.notFound("USER_NOT_FOUND", "User not found"));
    }
}
