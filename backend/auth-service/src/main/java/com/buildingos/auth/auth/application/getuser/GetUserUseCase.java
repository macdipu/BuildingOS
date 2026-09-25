package com.buildingos.auth.auth.application.getuser;

import com.buildingos.auth.auth.domain.model.User;

public interface GetUserUseCase {
    User execute(GetUserQuery query);
}
