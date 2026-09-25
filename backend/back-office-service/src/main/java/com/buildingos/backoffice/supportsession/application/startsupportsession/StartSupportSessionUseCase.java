package com.buildingos.backoffice.supportsession.application.startsupportsession;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.supportsession.application.SupportSessionDetails;

public interface StartSupportSessionUseCase {
    SupportSessionDetails execute(Actor actor, StartSupportSessionCommand input);
}
