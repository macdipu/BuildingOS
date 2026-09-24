package com.buildingos.building.unit.application.createunit;

import com.buildingos.building.unit.application.UnitInput;
import java.util.UUID;

public record CreateUnitCommand(UUID buildingId, UnitInput input, String reason) {}
