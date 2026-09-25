package com.buildingos.backoffice.supportsession.application.getsupportsession;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.supportsession.application.SupportSessionDetails;

public interface GetSupportSessionUseCase {
    SupportSessionDetails execute(Actor actor, GetSupportSessionQuery input);
}
