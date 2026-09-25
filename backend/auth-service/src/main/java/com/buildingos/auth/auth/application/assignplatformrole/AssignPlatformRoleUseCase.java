package com.buildingos.auth.auth.application.assignplatformrole;

import com.buildingos.auth.auth.domain.model.User;

public interface AssignPlatformRoleUseCase {
    User execute(AssignPlatformRoleCommand command);
}
