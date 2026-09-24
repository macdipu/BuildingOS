package com.buildingos.building.unit.application;

import com.buildingos.building.unit.domain.model.Floor;
import com.buildingos.building.unit.domain.model.Unit;

public record UnitView(Unit unit, Floor floor) {}
