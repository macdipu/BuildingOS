package com.buildingos.auth.auth.presentation.rest;

import com.buildingos.auth.auth.application.getpublickeys.GetPublicSigningKeysQuery;
import com.buildingos.auth.auth.application.getpublickeys.GetPublicSigningKeysUseCase;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Auth transport; provider and signing configuration fail closed at startup. */
@RestController
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
