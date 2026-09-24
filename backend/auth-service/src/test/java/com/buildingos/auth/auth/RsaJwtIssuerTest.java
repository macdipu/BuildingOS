package com.buildingos.auth.auth;

import com.buildingos.auth.auth.infrastructure.security.RsaJwtIssuer;
import com.buildingos.platform.web.security.SecuritySettings;
import com.nimbusds.jose.jwk.*;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jwt.SignedJWT;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.assertj.core.api.Assertions.*;

class RsaJwtIssuerTest {
    @TempDir Path dir;
    final SecuritySettings settings = new SecuritySettings("https://auth.example", "buildingos",
            "https://auth.example/.well-known/jwks.json", List.of(), List.of());
    final Clock clock = Clock.fixed(Instant.parse("2026-01-01T00:00:00Z"), ZoneOffset.UTC);

    String keyFile(List<JWK> keys) throws Exception {
        Path path = dir.resolve(UUID.randomUUID() + ".json");
        Files.writeString(path, new JWKSet(keys).toString(false));
        return path.toString();
    }
    RsaJwtIssuer issuer(String path, String kid) {
        return new RsaJwtIssuer(settings, clock, path, kid, Duration.ofMinutes(7), false);
    }

    @Test void rotatesActiveKeyWhileRetainingOldPublicKeyAndSurvivesRestart() throws Exception {
        var old = new RSAKeyGenerator(2048).keyID("old").generate();
        var active = new RSAKeyGenerator(2048).keyID("active").generate();
        String path = keyFile(List.of(old, active));
        var oldIssuer = issuer(path, "old");
        var current = issuer(path, "active");
        var oldToken = SignedJWT.parse(oldIssuer.issue(UUID.randomUUID(), "+8801700000000", Set.of()).accessToken());
        var issued = current.issue(UUID.randomUUID(), "+8801700000000", Set.of());
        var token = SignedJWT.parse(issued.accessToken());
        assertThat(token.getHeader().getKeyID()).isEqualTo("active");
        assertThat(issued.expiresInSeconds()).isEqualTo(420);
        assertThat(token.getJWTClaimsSet().getExpirationTime().toInstant()).isEqualTo(clock.instant().plusSeconds(420));
        var published = JWKSet.parse(Map.of("keys", issuer(path, "active").publicSigningKeys()));
        assertThat(published.getKeys()).hasSize(2).allMatch(k -> !k.isPrivate());
        assertThat(token.verify(new RSASSAVerifier((RSAKey) published.getKeyByKeyId("active")))).isTrue();
        assertThat(oldToken.verify(new RSASSAVerifier((RSAKey) published.getKeyByKeyId("old")))).isTrue();
        assertThat(published.toString()).doesNotContain("\"d\"", "\"p\"", "\"q\"");
    }

    @Test void rejectsMissingUnknownPublicOnlyShortAndDuplicateKeys() throws Exception {
        var key = new RSAKeyGenerator(2048).keyID("key").generate();
        String path = keyFile(List.of(key));
        assertThatThrownBy(() -> issuer("", "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> issuer(path, "")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> issuer(path, "missing")).isInstanceOf(IllegalArgumentException.class);
        String publicOnly = keyFile(List.of(key.toPublicJWK()));
        assertThatThrownBy(() -> issuer(publicOnly, "key")).isInstanceOf(IllegalArgumentException.class);
        String duplicate = keyFile(List.of(key, key));
        assertThatThrownBy(() -> issuer(duplicate, "key")).isInstanceOf(IllegalArgumentException.class);
        var generator = java.security.KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        var pair = generator.generateKeyPair();
        var shortKey = new RSAKey.Builder((java.security.interfaces.RSAPublicKey) pair.getPublic())
                .privateKey((java.security.interfaces.RSAPrivateKey) pair.getPrivate()).keyID("short").build();
        String shortPath = keyFile(List.of(shortKey));
        assertThatThrownBy(() -> issuer(shortPath, "short")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new RsaJwtIssuer(settings, clock, path, "key", Duration.ZERO, false))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test void generatedKeyFallbackIsOnlyForDevelopmentAndNeverForInvalidConfiguredFile() {
        assertThat(new RsaJwtIssuer(settings, clock, "", "", Duration.ofMinutes(15), true)
                .publicSigningKeys()).hasSize(1);
        assertThatThrownBy(() -> new RsaJwtIssuer(settings, clock, "/missing", "x", Duration.ofMinutes(15), true))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
