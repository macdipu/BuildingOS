package com.buildingos.building.membership.application.listmembers;

import java.util.UUID;

public record ListMembersQuery(UUID buildingId, int page, int size) {}
