package com.buildingos.auth.auth.application.getdirectoryuser;

import com.buildingos.auth.auth.domain.model.User;

public interface GetDirectoryUserUseCase {
    User execute(GetDirectoryUserQuery query);
}
