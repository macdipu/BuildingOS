package com.buildingos.building.unit.application.updatefloor;

import com.buildingos.building.unit.application.FloorInput;
import java.util.UUID;

public record UpdateFloorCommand(UUID buildingId, UUID floorId, FloorInput input, Long expectedVersion,
        String reason) {}
