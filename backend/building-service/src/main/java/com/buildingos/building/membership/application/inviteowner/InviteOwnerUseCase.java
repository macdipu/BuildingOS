package com.buildingos.building.membership.application.inviteowner;

import com.buildingos.building.shared.application.Actor;

public interface InviteOwnerUseCase {
    InvitationResult execute(Actor actor, InviteOwnerCommand command);
}
