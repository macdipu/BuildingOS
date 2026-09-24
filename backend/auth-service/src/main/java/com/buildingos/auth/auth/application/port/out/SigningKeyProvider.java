package com.buildingos.auth.auth.application.port.out;

import java.util.List;
import java.util.Map;

/**
 * Exposes the token issuer's verification keys. Implementations must return public key
 * material only, each key as a JWK JSON object (RFC 7517); private parameters never leave
 * the issuer.
 */
public interface SigningKeyProvider {
    List<Map<String, Object>> publicSigningKeys();
}
