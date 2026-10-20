package com.buildingos.backoffice.platformuser.application.listplatformusers;

import com.buildingos.backoffice.platformuser.application.port.out.PlatformUserAdmin;
import com.buildingos.backoffice.platformuser.domain.model.PlatformRoles;
import com.buildingos.backoffice.platformuser.domain.model.PlatformUser;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.Page;

/** SUPER_ADMIN or PLATFORM_ADMIN (BOC-05). */
public final class ListPlatformUsersService implements ListPlatformUsersUseCase {
    private final PlatformUserAdmin users;

    public ListPlatformUsersService(PlatformUserAdmin users) { this.users = users; }

    @Override
    public Page<PlatformUser> execute(Actor actor, ListPlatformUsersQuery query) {
        actor.requireOperator();
        Page.validate(query.page(), query.size());
        String role = query.role() == null || query.role().isBlank() ? null : PlatformRoles.require(query.role().trim());
        String text = query.query() == null || query.query().isBlank() ? null : query.query().trim();
        return users.list(text, role, query.page(), query.size());
    }
}
