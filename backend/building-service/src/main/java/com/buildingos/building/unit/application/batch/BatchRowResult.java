package com.buildingos.building.unit.application.batch;

import com.buildingos.building.unit.domain.model.Floor;
import com.buildingos.building.unit.domain.model.UnitDetails;
import java.util.List;

/** {@code details} is present only when the row has no errors; {@code floor} when it resolved. */
public record BatchRowResult(BatchRowInput input, Floor floor, UnitDetails details, List<RowError> errors) {
    public BatchRowResult {
        errors = List.copyOf(errors);
    }

    public boolean valid() { return errors.isEmpty(); }
}
