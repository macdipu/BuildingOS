package com.buildingos.backoffice.platformuser.infrastructure.config;

import com.buildingos.backoffice.platformuser.application.assignplatformrole.AssignPlatformRoleService;
import com.buildingos.backoffice.platformuser.application.assignplatformrole.AssignPlatformRoleUseCase;
import com.buildingos.backoffice.platformuser.application.getplatformuser.GetPlatformUserService;
import com.buildingos.backoffice.platformuser.application.getplatformuser.GetPlatformUserUseCase;
import com.buildingos.backoffice.platformuser.application.listplatformusers.ListPlatformUsersService;
import com.buildingos.backoffice.platformuser.application.listplatformusers.ListPlatformUsersUseCase;
import com.buildingos.backoffice.platformuser.application.port.out.PlatformUserAdmin;
import com.buildingos.backoffice.platformuser.application.revokeplatformrole.RevokePlatformRoleService;
import com.buildingos.backoffice.platformuser.application.revokeplatformrole.RevokePlatformRoleUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PlatformUserConfiguration {
    @Bean
    ListPlatformUsersUseCase listPlatformUsers(PlatformUserAdmin users) { return new ListPlatformUsersService(users); }

    @Bean
    GetPlatformUserUseCase getPlatformUser(PlatformUserAdmin users) { return new GetPlatformUserService(users); }

    @Bean
    AssignPlatformRoleUseCase assignPlatformRole(PlatformUserAdmin users) {
        return new AssignPlatformRoleService(users);
    }

    @Bean
    RevokePlatformRoleUseCase revokePlatformRole(PlatformUserAdmin users) {
        return new RevokePlatformRoleService(users);
    }
}
