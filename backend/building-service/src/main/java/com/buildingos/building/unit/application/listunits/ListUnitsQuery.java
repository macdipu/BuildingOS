package com.buildingos.building.unit.application.listunits;

import com.buildingos.building.unit.domain.model.UnitType;
import java.util.UUID;

/** {@code floorId}/{@code type} null = any. */
public record ListUnitsQuery(UUID buildingId, UUID floorId, UnitType type, int page, int size) {}
