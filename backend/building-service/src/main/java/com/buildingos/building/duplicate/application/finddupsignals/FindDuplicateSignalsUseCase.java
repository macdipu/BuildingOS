package com.buildingos.building.duplicate.application.finddupsignals;

import com.buildingos.building.duplicate.domain.model.DuplicateMatch;
import com.buildingos.building.shared.application.Actor;
import java.util.List;

public interface FindDuplicateSignalsUseCase {
    List<DuplicateMatch> execute(Actor actor, FindDuplicateSignalsQuery query);
}
