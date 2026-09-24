package com.buildingos.building.membership.application.claiminvitation;

import java.util.UUID;

public record ClaimInvitationCommand(UUID invitationId, UUID operationId) {}
