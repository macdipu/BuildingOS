package com.buildingos.backoffice.supportsession.application.checksupportscope;

import com.buildingos.backoffice.supportsession.domain.model.SupportScope;
import java.util.UUID;

public record CheckSupportScopeQuery(UUID sessionId, SupportScope scope) {}
