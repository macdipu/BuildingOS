package com.buildingos.building.unit.application.listunits;

import com.buildingos.building.unit.domain.model.UnitSearch;
import java.util.UUID;

/** {@code search.ownerUserId} is the requested owner filter; the caller's read scope still applies. */
public record ListUnitsQuery(UUID buildingId, UnitSearch search, int page, int size) {}
