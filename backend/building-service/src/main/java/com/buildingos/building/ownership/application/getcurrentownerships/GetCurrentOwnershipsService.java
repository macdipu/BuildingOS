package com.buildingos.building.ownership.application.getcurrentownerships;

import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.ownership.domain.model.UnitOwnership;
import com.buildingos.building.ownership.domain.repository.OwnershipRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.unit.application.UnitErrors;
import com.buildingos.building.unit.application.UnitReadScope;
import java.util.List;

/**
 * Current allocations and the ownership revision used as {@code expectedVersion}. An owner sees only their own
 * allocation, never co-owners'; a unit they do not currently own looks missing (UO-08).
 */
public final class GetCurrentOwnershipsService implements GetCurrentOwnershipsUseCase {
    private final BuildingAccess access;
    private final OwnershipRepository ownership;
    private final UnitOfWork unitOfWork;

    public GetCurrentOwnershipsService(BuildingAccess access, OwnershipRepository ownership, UnitOfWork unitOfWork) {
        this.access = access;
        this.ownership = ownership;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public UnitOwnership execute(Actor actor, GetCurrentOwnershipsQuery query) {
        return unitOfWork.inTransaction(() -> {
            var owner = UnitReadScope.ownerFilter(actor, access.requireMemberToRead(actor, query.buildingId()));
            var current = ownership.current(query.buildingId(), query.unitId()).orElseThrow(UnitErrors::unitNotFound);
            if (owner == null) {
                return current;
            }
            var mine = current.openFor(owner).orElseThrow(UnitErrors::unitNotFound);
            return new UnitOwnership(current.buildingId(), current.unitId(), current.revision(), List.of(mine));
        });
    }
}
