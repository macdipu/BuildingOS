package com.buildingos.building.unit.application;

import com.buildingos.building.unit.domain.model.Floor;
import com.buildingos.building.unit.domain.model.Unit;
import java.util.HashMap;
import java.util.Map;

public final class UnitAudit {
    public static final String FLOOR = "FLOOR";
    public static final String UNIT = "UNIT";

    private UnitAudit() {}

    public static Map<String, String> of(Floor f) {
        var d = f.details();
        return Map.of("label", d.label(), "kind", d.kind().name(), "displayOrder", Integer.toString(d.displayOrder()),
                "version", Long.toString(f.version()));
    }

    public static Map<String, String> of(Unit u) {
        var d = u.details();
        Map<String, String> fields = new HashMap<>();
        fields.put("number", d.number());
        fields.put("floorId", d.floorId().toString());
        fields.put("type", d.type().name());
        fields.put("areaSqft", d.areaSqft().toPlainString());
        fields.put("version", Long.toString(u.version()));
        if (d.bedrooms() != null) {
            fields.put("bedrooms", d.bedrooms().toString());
        }
        if (d.defaultMaintenanceRate() != null) {
            fields.put("defaultMaintenanceRate", d.defaultMaintenanceRate().toPlainString());
        }
        return fields;
    }
}
