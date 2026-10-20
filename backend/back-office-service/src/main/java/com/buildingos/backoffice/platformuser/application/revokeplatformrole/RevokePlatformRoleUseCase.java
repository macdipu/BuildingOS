package com.buildingos.backoffice.platformuser.application.revokeplatformrole;

import com.buildingos.backoffice.platformuser.domain.model.PlatformUser;
import com.buildingos.backoffice.shared.application.Actor;

public interface RevokePlatformRoleUseCase {
    PlatformUser execute(Actor actor, RevokePlatformRoleCommand command);
}
