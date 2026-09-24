package com.buildingos.building.building.application;

import java.util.UUID;

public record BuildingLifecycleCommand(UUID buildingId, String reason) {}
