package com.buildingos.building.membership.application.revokeinvitation;

import java.util.UUID;

public record RevokeInvitationCommand(UUID buildingId, UUID invitationId, String reason) {}
