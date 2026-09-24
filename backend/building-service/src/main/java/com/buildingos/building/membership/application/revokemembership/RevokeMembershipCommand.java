package com.buildingos.building.membership.application.revokemembership;

import java.util.UUID;

public record RevokeMembershipCommand(UUID buildingId, UUID membershipId, String reason, Long expectedVersion) {}
