package com.buildingos.building.unit.infrastructure.config;

import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import com.buildingos.building.unit.application.createfloor.CreateFloorService;
import com.buildingos.building.unit.application.createfloor.CreateFloorUseCase;
import com.buildingos.building.unit.application.createunit.CreateUnitService;
import com.buildingos.building.unit.application.createunit.CreateUnitUseCase;
import com.buildingos.building.unit.application.getunit.GetUnitService;
import com.buildingos.building.unit.application.getunit.GetUnitUseCase;
import com.buildingos.building.unit.application.listfloors.ListFloorsService;
import com.buildingos.building.unit.application.listfloors.ListFloorsUseCase;
import com.buildingos.building.unit.application.listunits.ListUnitsService;
import com.buildingos.building.unit.application.listunits.ListUnitsUseCase;
import com.buildingos.building.unit.application.updatefloor.UpdateFloorService;
import com.buildingos.building.unit.application.updatefloor.UpdateFloorUseCase;
import com.buildingos.building.unit.application.updateunit.UpdateUnitService;
import com.buildingos.building.unit.application.updateunit.UpdateUnitUseCase;
import com.buildingos.building.unit.domain.repository.FloorRepository;
import com.buildingos.building.unit.domain.repository.UnitRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class UnitConfiguration {
    @Bean
    CreateFloorUseCase createFloor(BuildingAccess access, FloorRepository floors, AuditRepository audit,
            UnitOfWork uow, Clock clock) {
        return new CreateFloorService(access, floors, audit, uow, clock);
    }

    @Bean
    UpdateFloorUseCase updateFloor(BuildingAccess access, FloorRepository floors, AuditRepository audit,
            UnitOfWork uow, Clock clock) {
        return new UpdateFloorService(access, floors, audit, uow, clock);
    }

    @Bean
    ListFloorsUseCase listFloors(BuildingAccess access, FloorRepository floors, UnitOfWork uow) {
        return new ListFloorsService(access, floors, uow);
    }

    @Bean
    CreateUnitUseCase createUnit(BuildingAccess access, FloorRepository floors, UnitRepository units,
            AuditRepository audit, UnitOfWork uow, Clock clock) {
        return new CreateUnitService(access, floors, units, audit, uow, clock);
    }

    @Bean
    UpdateUnitUseCase updateUnit(BuildingAccess access, FloorRepository floors, UnitRepository units,
            AuditRepository audit, UnitOfWork uow, Clock clock) {
        return new UpdateUnitService(access, floors, units, audit, uow, clock);
    }

    @Bean
    ListUnitsUseCase listUnits(BuildingAccess access, FloorRepository floors, UnitRepository units, UnitOfWork uow) {
        return new ListUnitsService(access, floors, units, uow);
    }

    @Bean
    GetUnitUseCase getUnit(BuildingAccess access, FloorRepository floors, UnitRepository units, UnitOfWork uow) {
        return new GetUnitService(access, floors, units, uow);
    }
}
