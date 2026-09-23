package com.buildingos.subscription.subscription.application.getmyentitlements;

import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.shared.application.Actor;

public interface GetMyEntitlementsUseCase {
    Entitlements execute(Actor actor);
}
