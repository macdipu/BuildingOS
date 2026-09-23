package com.buildingos.subscription.freetier.application.getfreetier;

import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.shared.application.Actor;

public interface GetFreeTierUseCase {
    Entitlements execute(Actor actor);
}
