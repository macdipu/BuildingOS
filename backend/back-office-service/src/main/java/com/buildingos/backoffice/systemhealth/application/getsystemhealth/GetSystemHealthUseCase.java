package com.buildingos.backoffice.systemhealth.application.getsystemhealth;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.systemhealth.domain.model.SystemHealth;

public interface GetSystemHealthUseCase {
    SystemHealth execute(Actor actor);
}
