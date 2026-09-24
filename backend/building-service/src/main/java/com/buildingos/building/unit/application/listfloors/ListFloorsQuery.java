package com.buildingos.building.unit.application.listfloors;

import java.util.UUID;

public record ListFloorsQuery(UUID buildingId, int page, int size) {}
