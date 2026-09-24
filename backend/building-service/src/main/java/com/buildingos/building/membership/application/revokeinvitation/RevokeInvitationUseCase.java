package com.buildingos.building.membership.application.revokeinvitation;

import com.buildingos.building.membership.application.InvitationView;
import com.buildingos.building.shared.application.Actor;

public interface RevokeInvitationUseCase {
    InvitationView execute(Actor actor, RevokeInvitationCommand command);
}
