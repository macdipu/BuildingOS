package com.buildingos.building.membership.application.listinvitations;

import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.membership.application.InvitationView;
import com.buildingos.building.membership.domain.repository.BuildingInvitationRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import java.time.Clock;

public final class ListInvitationsService implements ListInvitationsUseCase {
    private final BuildingAccess access;
    private final BuildingInvitationRepository invitations;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public ListInvitationsService(BuildingAccess access, BuildingInvitationRepository invitations,
            UnitOfWork unitOfWork, Clock clock) {
        this.access = access;
        this.invitations = invitations;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Page<InvitationView> execute(Actor actor, ListInvitationsQuery query) {
        Page.validate(query.page(), query.size());
        return unitOfWork.inTransaction(() -> {
            access.requireAdminToRead(actor, query.buildingId());
            var now = clock.instant();
            var items = invitations.findByBuilding(query.buildingId(), query.page(), query.size()).stream()
                    .map(i -> InvitationView.at(i, now)).toList();
            return new Page<>(items, query.page(), query.size(), invitations.countByBuilding(query.buildingId()));
        });
    }
}
