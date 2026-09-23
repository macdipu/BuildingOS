package com.buildingos.subscription.freetier.application.updatefreetier;

import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.shared.application.Actor;

public interface UpdateFreeTierUseCase {
    Entitlements execute(Actor actor, UpdateFreeTierCommand command);
}
