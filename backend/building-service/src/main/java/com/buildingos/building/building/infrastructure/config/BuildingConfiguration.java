package com.buildingos.building.building.infrastructure.config;

import com.buildingos.building.building.application.BuildingChanges;
import com.buildingos.building.building.application.activatebuilding.ActivateBuildingService;
import com.buildingos.building.building.application.activatebuilding.ActivateBuildingUseCase;
import com.buildingos.building.building.application.getbuilding.GetBuildingService;
import com.buildingos.building.building.application.getbuilding.GetBuildingUseCase;
import com.buildingos.building.building.application.reactivatebuilding.ReactivateBuildingService;
import com.buildingos.building.building.application.reactivatebuilding.ReactivateBuildingUseCase;
import com.buildingos.building.building.application.suspendbuilding.SuspendBuildingService;
import com.buildingos.building.building.application.suspendbuilding.SuspendBuildingUseCase;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.building.domain.repository.BuildingRepository;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BuildingConfiguration {
    @Bean
    BuildingChanges buildingChanges(BuildingRepository buildings, LifecycleTransitionRepository transitions,
            UnitOfWork uow, Clock clock) {
        return new BuildingChanges(buildings, transitions, uow, clock);
    }

    @Bean
    ActivateBuildingUseCase activateBuilding(BuildingChanges changes, BuildingMembershipRepository memberships) {
        return new ActivateBuildingService(changes, memberships);
    }

    @Bean
    SuspendBuildingUseCase suspendBuilding(BuildingChanges changes) { return new SuspendBuildingService(changes); }

    @Bean
    ReactivateBuildingUseCase reactivateBuilding(BuildingChanges changes) {
        return new ReactivateBuildingService(changes);
    }

    @Bean
    GetBuildingUseCase getBuilding(BuildingRepository buildings, BuildingMembershipRepository memberships) {
        return new GetBuildingService(buildings, memberships);
    }
}
