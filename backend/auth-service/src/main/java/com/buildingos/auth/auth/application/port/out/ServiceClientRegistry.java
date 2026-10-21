package com.buildingos.auth.auth.application.port.out;

import java.util.Optional;
import java.util.Set;

/** Configured service clients (D-38). Never logs or returns the secret. */
public interface ServiceClientRegistry {
    /** The client's granted scopes when id and secret match a configured client; empty otherwise. */
    Optional<Set<String>> authenticate(String clientId, String clientSecret);
}
