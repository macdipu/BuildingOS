package com.buildingos.building.unit.application.previewunitbatch;

import java.util.UUID;

public record PreviewUnitBatchCommand(UUID buildingId, BatchSource source) {}
