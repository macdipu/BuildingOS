package com.buildingos.auth.auth.application.getdirectoryuser;

import java.util.Set;
import java.util.UUID;

public record GetDirectoryUserQuery(Set<String> callerServiceScopes, UUID userId) {
    public GetDirectoryUserQuery {
        callerServiceScopes = Set.copyOf(callerServiceScopes);
    }
}
