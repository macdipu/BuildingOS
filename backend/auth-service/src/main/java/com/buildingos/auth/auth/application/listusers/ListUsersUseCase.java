package com.buildingos.auth.auth.application.listusers;

import com.buildingos.auth.auth.application.Page;
import com.buildingos.auth.auth.domain.model.User;

public interface ListUsersUseCase {
    Page<User> execute(ListUsersQuery query);
}
