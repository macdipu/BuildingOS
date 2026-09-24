package com.buildingos.auth.auth.application.getpublickeys;

import java.util.List;
import java.util.Map;

/** Public halves of the token signing keys, each as a JWK JSON object (RFC 7517). */
public record GetPublicSigningKeysResult(List<Map<String, Object>> keys) {
    public GetPublicSigningKeysResult {
        keys = List.copyOf(keys);
    }
}
