package com.buildingos.building.buildingapplication.application.getapplicationhistory;

import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.domain.model.LifecycleTransition;
import java.util.List;

public interface GetApplicationHistoryUseCase {
    List<LifecycleTransition> execute(Actor actor, GetApplicationHistoryQuery query);
}
