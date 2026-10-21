package com.buildingos.auth.auth.application.port.out;

import com.buildingos.auth.auth.application.port.out.TokenIssuer.IssuedToken;
import java.time.Duration;
import java.util.Set;

/** Service-to-service tokens (BOS-010 D-38): {@code sub} = {@code service:<clientId>}, scopes only, no platform roles. */
public interface ServiceTokenIssuer {
    IssuedToken issueService(String clientId, Set<String> scopes, Duration ttl);
}
