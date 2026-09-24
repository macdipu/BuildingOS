package com.buildingos.building.membership.application.inviteowner;

import java.util.UUID;

/** {@code phone} in any accepted Bangladesh form; canonicalized before use. */
public record InviteOwnerCommand(UUID buildingId, String phone, String reason) {}
