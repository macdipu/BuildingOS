package com.buildingos.account.auth.presentation.rest;

import com.buildingos.account.auth.application.getpublickeys.GetPublicSigningKeysQuery;
import com.buildingos.account.auth.application.getpublickeys.GetPublicSigningKeysUseCase;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publishes the local issuer's public key set (RFC 7517 JWK Set). Local/test only, like the issuer. */
@RestController
@Profile({"local", "test"})
public final class JwkSetController {
    private final GetPublicSigningKeysUseCase publicSigningKeys;

    public JwkSetController(GetPublicSigningKeysUseCase publicSigningKeys) {
        this.publicSigningKeys = publicSigningKeys;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return Map.of("keys", publicSigningKeys.execute(new GetPublicSigningKeysQuery()).keys());
    }
}
