package com.buildingos.backoffice.platformuser.application.getplatformuser;

import com.buildingos.backoffice.platformuser.domain.model.PlatformUser;
import com.buildingos.backoffice.shared.application.Actor;
import java.util.UUID;

public interface GetPlatformUserUseCase {
    PlatformUser execute(Actor actor, UUID userId);
}
