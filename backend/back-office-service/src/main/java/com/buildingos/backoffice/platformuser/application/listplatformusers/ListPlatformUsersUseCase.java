package com.buildingos.backoffice.platformuser.application.listplatformusers;

import com.buildingos.backoffice.platformuser.domain.model.PlatformUser;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.Page;

public interface ListPlatformUsersUseCase {
    Page<PlatformUser> execute(Actor actor, ListPlatformUsersQuery query);
}
