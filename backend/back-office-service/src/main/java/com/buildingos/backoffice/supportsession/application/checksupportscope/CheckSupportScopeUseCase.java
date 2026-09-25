package com.buildingos.backoffice.supportsession.application.checksupportscope;

import com.buildingos.backoffice.shared.application.Actor;

public interface CheckSupportScopeUseCase {
    SupportScopeCheck execute(Actor actor, CheckSupportScopeQuery input);
}
