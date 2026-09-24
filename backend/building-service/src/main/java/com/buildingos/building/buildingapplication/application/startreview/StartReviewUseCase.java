package com.buildingos.building.buildingapplication.application.startreview;

import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;

public interface StartReviewUseCase {
    BuildingApplication execute(Actor actor, StartReviewCommand command);
}
