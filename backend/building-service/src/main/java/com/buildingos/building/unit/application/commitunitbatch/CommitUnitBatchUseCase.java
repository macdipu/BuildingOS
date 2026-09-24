package com.buildingos.building.unit.application.commitunitbatch;

import com.buildingos.building.shared.application.Actor;

public interface CommitUnitBatchUseCase {
    CommitResult execute(Actor actor, CommitUnitBatchCommand command);
}
