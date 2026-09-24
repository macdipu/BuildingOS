package com.buildingos.building.unit.application.createfloor;

import com.buildingos.building.unit.application.FloorInput;
import java.util.UUID;

/** {@code reason} optional for building admins, required for platform admins. */
public record CreateFloorCommand(UUID buildingId, FloorInput input, String reason) {}
