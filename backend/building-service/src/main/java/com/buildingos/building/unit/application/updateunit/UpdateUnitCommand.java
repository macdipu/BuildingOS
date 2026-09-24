package com.buildingos.building.unit.application.updateunit;

import com.buildingos.building.unit.application.UnitInput;
import java.util.UUID;

public record UpdateUnitCommand(UUID buildingId, UUID unitId, UnitInput input, Long expectedVersion, String reason) {}
