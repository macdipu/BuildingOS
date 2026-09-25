package com.buildingos.backoffice.supportsession.application.endsupportsession;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.supportsession.application.SupportSessionDetails;

public interface EndSupportSessionUseCase {
    SupportSessionDetails execute(Actor actor, EndSupportSessionCommand input);
}
