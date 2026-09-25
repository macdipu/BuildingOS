package com.buildingos.backoffice.supportsession.application.checksupportscope;

import com.buildingos.backoffice.supportsession.domain.model.ScopeCheckResult;
import com.buildingos.backoffice.supportsession.domain.model.SupportScope;
import java.util.UUID;

public record SupportScopeCheck(UUID sessionId, SupportScope scope, ScopeCheckResult result) {}
