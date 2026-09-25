package com.buildingos.backoffice.supportsession.presentation.rest.response;

import com.buildingos.backoffice.supportsession.application.checksupportscope.SupportScopeCheck;
import java.util.UUID;

public record ScopeCheckResponse(UUID sessionId, String scope, String result) {
    public static ScopeCheckResponse of(SupportScopeCheck check) {
        return new ScopeCheckResponse(check.sessionId(), check.scope().name(), check.result().name());
    }
}
