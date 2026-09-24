package com.buildingos.building.membership.application.inviteowner;

import com.buildingos.building.membership.application.InvitationView;

/** {@code created} is false when a live invitation for the same phone already existed and is returned instead. */
public record InvitationResult(InvitationView invitation, boolean created) {}
