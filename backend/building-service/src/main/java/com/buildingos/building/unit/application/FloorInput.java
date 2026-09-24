package com.buildingos.building.unit.application;

import com.buildingos.building.unit.domain.model.FloorDetails;
import com.buildingos.building.unit.domain.model.FloorKind;

public record FloorInput(String label, FloorKind kind, Integer displayOrder) {
    public FloorDetails details() {
        return FloorDetails.of(label, kind, displayOrder);
    }
}
