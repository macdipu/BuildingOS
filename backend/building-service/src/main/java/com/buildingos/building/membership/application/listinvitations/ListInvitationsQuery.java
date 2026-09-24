package com.buildingos.building.membership.application.listinvitations;

import java.util.UUID;

public record ListInvitationsQuery(UUID buildingId, int page, int size) {}
