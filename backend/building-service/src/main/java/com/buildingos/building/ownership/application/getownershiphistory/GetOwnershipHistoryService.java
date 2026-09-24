package com.buildingos.building.ownership.application.getownershiphistory;

import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.ownership.domain.model.OwnershipHistory;
import com.buildingos.building.ownership.domain.repository.OwnershipRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.NotPermittedException;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.unit.application.UnitErrors;

/**
 * Admins see a unit's full history; an owner with active membership sees only their own periods and transfers,
 * including former ownership (DECISIONS "History visibility"). Units they never owned look missing.
 */
public final class GetOwnershipHistoryService implements GetOwnershipHistoryUseCase {
    private final BuildingAccess access;
    private final OwnershipRepository ownership;
    private final UnitOfWork unitOfWork;

    public GetOwnershipHistoryService(BuildingAccess access, OwnershipRepository ownership, UnitOfWork unitOfWork) {
        this.access = access;
        this.ownership = ownership;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public OwnershipHistory execute(Actor actor, GetOwnershipHistoryQuery query) {
        return unitOfWork.inTransaction(() -> {
            var grant = access.requireMemberToRead(actor, query.buildingId());
            if (grant.isAdmin()) {
                return ownership.history(query.buildingId(), query.unitId(), null)
                        .orElseThrow(UnitErrors::unitNotFound);
            }
            if (!grant.roles().contains(BuildingRole.OWNER)) {
                throw new NotPermittedException();
            }
            return ownership.history(query.buildingId(), query.unitId(), actor.userId())
                    .filter(h -> !h.periods().isEmpty()).orElseThrow(UnitErrors::unitNotFound);
        });
    }
}
