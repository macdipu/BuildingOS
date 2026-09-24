package com.buildingos.building.unit.application.previewunitbatch;

import com.buildingos.building.unit.application.batch.BatchPreview;
import com.buildingos.building.shared.application.Actor;

public interface PreviewUnitBatchUseCase {
    BatchPreview execute(Actor actor, PreviewUnitBatchCommand command);
}
