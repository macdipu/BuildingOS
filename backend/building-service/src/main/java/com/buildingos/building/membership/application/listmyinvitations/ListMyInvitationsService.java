package com.buildingos.building.membership.application.listmyinvitations;

import com.buildingos.building.membership.application.VerifiedPhoneIdentity;
import com.buildingos.building.building.domain.repository.BuildingRepository;
import com.buildingos.building.membership.domain.repository.BuildingInvitationRepository;
import java.time.Clock;
import java.util.List;

/** Live invitations addressed to the caller's verified phone; no SMS delivery in F4, they appear after login. */
public final class ListMyInvitationsService implements ListMyInvitationsUseCase {
    private final BuildingInvitationRepository invitations;
    private final BuildingRepository buildings;
    private final Clock clock;

    public ListMyInvitationsService(BuildingInvitationRepository invitations, BuildingRepository buildings,
            Clock clock) {
        this.invitations = invitations;
        this.buildings = buildings;
        this.clock = clock;
    }

    @Override
    public List<MyInvitation> execute(VerifiedPhoneIdentity identity) {
        return invitations.findLiveForPhone(identity.phone(), clock.instant()).stream()
                .flatMap(i -> buildings.findById(i.buildingId()).map(b -> new MyInvitation(i, b)).stream())
                .toList();
    }
}
