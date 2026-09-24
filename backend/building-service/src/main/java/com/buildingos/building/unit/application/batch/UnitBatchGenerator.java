package com.buildingos.building.unit.application.batch;

import com.buildingos.building.unit.application.UnitErrors;
import com.buildingos.building.unit.domain.model.BatchRejectedException;
import com.buildingos.building.unit.domain.model.Floor;
import com.buildingos.building.unit.domain.model.NumberPattern;
import com.buildingos.building.unit.domain.model.Unit;
import com.buildingos.building.unit.domain.repository.FloorRepository;
import com.buildingos.building.unit.domain.repository.UnitRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/** Expands a {@link GenerateSpec} into raw rows; the batch validator then checks them like any other input. */
public final class UnitBatchGenerator {
    private final FloorRepository floors;
    private final UnitRepository units;

    public UnitBatchGenerator(FloorRepository floors, UnitRepository units) {
        this.floors = floors;
        this.units = units;
    }

    public List<BatchRowInput> generate(UUID buildingId, GenerateSpec spec, BatchLimits limits) {
        if (spec.floorIds() == null || spec.floorIds().isEmpty()) {
            throw BatchRejectedException.invalid("FLOORS_REQUIRED", "floorIds is required");
        }
        var pattern = new NumberPattern(spec.numberPattern());
        int start = spec.start() == null ? 1 : spec.start();
        List<Floor> targets = spec.floorIds().stream()
                .map(id -> floors.findInBuilding(buildingId, id).orElseThrow(UnitErrors::floorNotFound)).toList();
        List<Unit> template = spec.templateFloorId() == null ? List.of() : templateUnits(buildingId, spec, limits);
        int perFloor = spec.templateFloorId() == null ? perFloor(spec, limits) : template.size();
        if ((long) perFloor * targets.size() > limits.maxRows()) {
            throw BatchRejectedException.invalid("BATCH_TOO_LARGE",
                    "A batch allows at most " + limits.maxRows() + " rows");
        }
        List<BatchRowInput> rows = new ArrayList<>();
        for (var floor : targets) {
            for (int i = 0; i < perFloor; i++) {
                String number = pattern.apply(floor.details().displayOrder(), i, start);
                rows.add(template.isEmpty() ? fromSpec(rows.size() + 1, number, floor, spec)
                        : fromTemplate(rows.size() + 1, number, floor, template.get(i)));
            }
        }
        return rows;
    }

    private List<Unit> templateUnits(UUID buildingId, GenerateSpec spec, BatchLimits limits) {
        floors.findInBuilding(buildingId, spec.templateFloorId()).orElseThrow(UnitErrors::floorNotFound);
        var template = units.findByBuilding(buildingId, spec.templateFloorId(), null, 0, limits.maxRows());
        if (template.isEmpty()) {
            throw BatchRejectedException.invalid("TEMPLATE_FLOOR_EMPTY", "The template floor has no units");
        }
        return template;
    }

    private static int perFloor(GenerateSpec spec, BatchLimits limits) {
        if (spec.unitsPerFloor() == null || spec.unitsPerFloor() < 1 || spec.unitsPerFloor() > limits.maxRows()) {
            throw BatchRejectedException.invalid("UNITS_PER_FLOOR_INVALID",
                    "unitsPerFloor must be between 1 and " + limits.maxRows());
        }
        return spec.unitsPerFloor();
    }

    private static BatchRowInput fromSpec(int row, String number, Floor floor, GenerateSpec spec) {
        return new BatchRowInput(row, number, floor.id(), null, spec.type(), text(spec.areaSqft()),
                spec.bedrooms() == null ? null : spec.bedrooms().toString(), text(spec.defaultMaintenanceRate()), null);
    }

    private static BatchRowInput fromTemplate(int row, String number, Floor floor, Unit source) {
        var d = source.details();
        return new BatchRowInput(row, number, floor.id(), null, d.type().name(), text(d.areaSqft()),
                d.bedrooms() == null ? null : d.bedrooms().toString(), text(d.defaultMaintenanceRate()), d.notes());
    }

    private static String text(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
