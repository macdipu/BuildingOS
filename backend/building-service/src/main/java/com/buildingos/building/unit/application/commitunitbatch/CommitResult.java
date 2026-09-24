package com.buildingos.building.unit.application.commitunitbatch;

import com.buildingos.building.unit.application.UnitView;
import com.buildingos.building.unit.application.batch.BatchPreview;
import com.buildingos.building.unit.domain.model.UnitBatch;
import java.util.List;

/** A confirmed batch creates every row or none; a rejection carries each row's errors. */
public sealed interface CommitResult {
    record Committed(UnitBatch batch, List<UnitView> units, boolean replayed) implements CommitResult {
        public Committed {
            units = List.copyOf(units);
        }
    }

    record Rejected(BatchPreview preview) implements CommitResult {}
}
