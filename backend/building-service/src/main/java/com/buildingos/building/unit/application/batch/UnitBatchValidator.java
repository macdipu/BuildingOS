package com.buildingos.building.unit.application.batch;

import com.buildingos.building.unit.domain.model.BatchRejectedException;
import com.buildingos.building.unit.domain.model.Floor;
import com.buildingos.building.unit.domain.model.InvalidUnitInputException;
import com.buildingos.building.unit.domain.model.UnitDetails;
import com.buildingos.building.unit.domain.model.UnitNames;
import com.buildingos.building.unit.domain.model.UnitType;
import com.buildingos.building.unit.domain.repository.FloorRepository;
import com.buildingos.building.unit.domain.repository.UnitRepository;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Row validation shared by preview and commit (UO-04): the F4-T2 unit validator, floor resolution, duplicates
 * within the batch and against existing units. Every row is reported; none is dropped.
 */
public final class UnitBatchValidator {
    private final FloorRepository floors;
    private final UnitRepository units;

    public UnitBatchValidator(FloorRepository floors, UnitRepository units) {
        this.floors = floors;
        this.units = units;
    }

    public BatchPreview validate(UUID buildingId, List<BatchRowInput> rows, BatchLimits limits) {
        if (rows.isEmpty()) {
            throw BatchRejectedException.invalid("BATCH_EMPTY", "The batch has no rows");
        }
        if (rows.size() > limits.maxRows()) {
            throw BatchRejectedException.invalid("BATCH_TOO_LARGE",
                    "A batch allows at most " + limits.maxRows() + " rows");
        }
        var buildingFloors = floors.findByBuilding(buildingId);
        Map<UUID, Floor> byId = buildingFloors.stream().collect(Collectors.toMap(Floor::id, Function.identity()));
        Map<String, Floor> byLabel = buildingFloors.stream()
                .collect(Collectors.toMap(f -> f.details().normalizedLabel(), Function.identity()));

        List<BatchRowResult> checked = rows.stream().map(row -> check(row, byId, byLabel)).toList();
        Set<String> seen = new HashSet<>();
        List<BatchRowResult> deduplicated = new ArrayList<>();
        for (var result : checked) {
            if (result.valid() && !seen.add(result.details().normalizedNumber())) {
                result = reject(result, "number", "DUPLICATE_IN_BATCH", "Unit number repeats an earlier row");
            }
            deduplicated.add(result);
        }
        var taken = units.takenNumbers(buildingId, deduplicated.stream().filter(BatchRowResult::valid)
                .map(r -> r.details().normalizedNumber()).toList());
        return new BatchPreview(deduplicated.stream()
                .map(r -> r.valid() && taken.contains(r.details().normalizedNumber())
                        ? reject(r, "number", "UNIT_NUMBER_TAKEN", "Unit number already exists in this building")
                        : r)
                .toList());
    }

    private static BatchRowResult check(BatchRowInput row, Map<UUID, Floor> byId, Map<String, Floor> byLabel) {
        List<RowError> errors = new ArrayList<>();
        Floor floor = null;
        if (row.floorId() != null) {
            floor = byId.get(row.floorId());
        } else if (row.floorLabel() != null && !row.floorLabel().isBlank()) {
            floor = byLabel.get(UnitNames.normalized(row.floorLabel().strip()));
        } else {
            errors.add(new RowError("floor", "FLOOR_REQUIRED", "floor is required"));
        }
        if (floor == null && errors.isEmpty()) {
            errors.add(new RowError("floor", "FLOOR_NOT_FOUND", "Floor does not exist in this building"));
        }
        UnitType type = parseType(row.type(), errors);
        BigDecimal area = decimal(row.areaSqft(), "areaSqft", "AREA_INVALID_NUMBER", errors);
        Integer bedrooms = integer(row.bedrooms(), errors);
        BigDecimal rate = decimal(row.defaultMaintenanceRate(), "defaultMaintenanceRate",
                "MAINTENANCE_RATE_INVALID_NUMBER", errors);
        if (!errors.isEmpty()) {
            return new BatchRowResult(row, floor, null, errors);
        }
        try {
            return new BatchRowResult(row, floor,
                    new UnitDetails(row.number(), floor.id(), type, area, bedrooms, rate, row.notes()), List.of());
        } catch (InvalidUnitInputException invalid) {
            return new BatchRowResult(row, floor, null,
                    List.of(new RowError(invalid.field(), invalid.code(), invalid.getMessage())));
        }
    }

    private static BatchRowResult reject(BatchRowResult result, String field, String code, String message) {
        return new BatchRowResult(result.input(), result.floor(), null, List.of(new RowError(field, code, message)));
    }

    private static UnitType parseType(String raw, List<RowError> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return UnitType.valueOf(raw.strip().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException unknown) {
            errors.add(new RowError("type", "UNIT_TYPE_INVALID", "Unknown unit type: " + raw));
            return null;
        }
    }

    private static BigDecimal decimal(String raw, String field, String code, List<RowError> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(raw.strip());
        } catch (NumberFormatException notANumber) {
            errors.add(new RowError(field, code, field + " must be a number"));
            return null;
        }
    }

    private static Integer integer(String raw, List<RowError> errors) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(raw.strip());
        } catch (NumberFormatException notANumber) {
            errors.add(new RowError("bedrooms", "BEDROOMS_INVALID", "bedrooms must be a whole number"));
            return null;
        }
    }
}
