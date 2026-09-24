package com.buildingos.building.membership.application.getbuildingcontext;

import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;

public final class GetBuildingContextService implements GetBuildingContextUseCase {
    private final BuildingAccess access;
    private final UnitOfWork unitOfWork;

    public GetBuildingContextService(BuildingAccess access, UnitOfWork unitOfWork) {
        this.access = access;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public BuildingContext execute(Actor actor, GetBuildingContextQuery query) {
        return unitOfWork.inTransaction(() -> new BuildingContext(access.requireMemberToRead(actor, query.buildingId())));
    }
}
