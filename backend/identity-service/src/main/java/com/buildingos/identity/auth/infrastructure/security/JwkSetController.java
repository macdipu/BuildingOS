package com.buildingos.identity.auth.infrastructure.security;

import com.nimbusds.jose.jwk.JWKSet;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Publishes the local issuer's public key set. Local/test only; see {@link LocalRsaJwtIssuer}. */
@RestController
@Profile({"local", "test"})
public final class JwkSetController {
    private final LocalRsaJwtIssuer issuer;

    public JwkSetController(LocalRsaJwtIssuer issuer) {
        this.issuer = issuer;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        return new JWKSet(issuer.publicJwk()).toJSONObject();
    }
}
