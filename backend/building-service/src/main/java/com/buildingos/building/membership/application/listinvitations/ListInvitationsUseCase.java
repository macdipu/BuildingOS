package com.buildingos.building.membership.application.listinvitations;

import com.buildingos.building.membership.application.InvitationView;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.Page;

public interface ListInvitationsUseCase {
    Page<InvitationView> execute(Actor actor, ListInvitationsQuery query);
}
