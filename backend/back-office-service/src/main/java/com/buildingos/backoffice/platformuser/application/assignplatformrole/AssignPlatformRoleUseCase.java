package com.buildingos.backoffice.platformuser.application.assignplatformrole;

import com.buildingos.backoffice.platformuser.domain.model.PlatformUser;
import com.buildingos.backoffice.shared.application.Actor;

public interface AssignPlatformRoleUseCase {
    PlatformUser execute(Actor actor, AssignPlatformRoleCommand command);
}
