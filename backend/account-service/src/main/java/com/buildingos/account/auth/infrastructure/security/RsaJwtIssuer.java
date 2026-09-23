package com.buildingos.account.auth.infrastructure.security;

import com.buildingos.account.auth.application.port.out.SigningKeyProvider;
import com.buildingos.account.auth.application.port.out.TokenIssuer;
import com.buildingos.account.auth.domain.model.PlatformRole;
import com.buildingos.platform.web.security.SecuritySettings;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.KeyUse;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Signs using a validated active key; publishes only public parts of the retained set. */
public final class RsaJwtIssuer implements TokenIssuer, SigningKeyProvider {
    private final Duration accessTokenTtl;
    private final List<RSAKey> keys;
    private final RSAKey signingKey;
    private final SecuritySettings settings;
    private final Clock clock;

    public RsaJwtIssuer(SecuritySettings settings, Clock clock, String path, String activeKid,
            Duration ttl, boolean development) {
        this.settings = settings;
        this.clock = clock;
        if (ttl == null || ttl.toSeconds() < 1) throw new IllegalArgumentException("Token TTL must be positive");
        this.accessTokenTtl = ttl;
        try {
            if (path == null || path.isBlank()) {
                if (!development || (activeKid != null && !activeKid.isBlank())) {
                    throw new IllegalArgumentException("Signing key file and active kid are required");
                }
                this.keys = List.of(generateKey());
                this.signingKey = keys.get(0);
            } else {
                if (activeKid == null || activeKid.isBlank()) {
                    throw new IllegalArgumentException("Active signing kid is required");
                }
                var loaded = JWKSet.parse(Files.readString(Path.of(path)));
                var validated = new ArrayList<RSAKey>();
                var kids = new HashSet<String>();
                for (var key : loaded.getKeys()) {
                    if (!(key instanceof RSAKey rsa) || rsa.size() < 2048
                            || rsa.getKeyID() == null || rsa.getKeyID().isBlank() || !kids.add(rsa.getKeyID())
                            || (rsa.getAlgorithm() != null && !JWSAlgorithm.RS256.equals(rsa.getAlgorithm()))
                            || (rsa.getKeyUse() != null && !KeyUse.SIGNATURE.equals(rsa.getKeyUse()))) {
                        throw new IllegalArgumentException("Invalid signing key set");
                    }
                    validated.add(rsa);
                }
                this.keys = List.copyOf(validated);
                this.signingKey = keys.stream().filter(k -> activeKid.equals(k.getKeyID())).findFirst()
                        .orElseThrow(() -> new IllegalArgumentException("Active signing kid was not found"));
                if (!signingKey.isPrivate()) throw new IllegalArgumentException("Active key must contain private material");
                // Prove the private/public pair matches before serving requests.
                var probe = new SignedJWT(new JWSHeader(JWSAlgorithm.RS256), new JWTClaimsSet.Builder().build());
                probe.sign(new RSASSASigner(signingKey));
                if (!probe.verify(new RSASSAVerifier(signingKey.toPublicJWK()))) {
                    throw new IllegalArgumentException("Signing key pair does not match");
                }
            }
        } catch (Exception invalid) {
            // Do not expose parsed secret contents or secret-file contents in a cause chain.
            throw new IllegalArgumentException("Invalid account signing configuration");
        }
    }

    @Override
    public List<Map<String, Object>> publicSigningKeys() {
        return keys.stream().map(key -> key.toPublicJWK().toJSONObject()).toList();
    }

    @Override
    public IssuedToken issue(UUID userId, String phone, Set<PlatformRole> platformRoles) {
        Instant now = clock.instant();
        Instant expiry = now.plus(accessTokenTtl);
        List<String> scopes = platformRoles.stream().map(role -> "platform_role." + role.name())
                .collect(Collectors.toList());
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(settings.issuer())
                .audience(settings.audience())
                .subject(userId.toString())
                .claim("phone", phone)
                .claim("platform_roles", platformRoles.stream().map(Enum::name).toList())
                .claim("scope", String.join(" ", scopes))
                .issueTime(Date.from(now))
                .expirationTime(Date.from(expiry))
                .build();
        try {
            SignedJWT jwt = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
            jwt.sign(new RSASSASigner(signingKey));
            return new IssuedToken(jwt.serialize(), accessTokenTtl.toSeconds());
        } catch (Exception signingFailure) {
            throw new IllegalStateException("Failed to sign access token", signingFailure);
        }
    }

    private static RSAKey generateKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            var pair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                    .privateKey((RSAPrivateKey) pair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate local signing key", e);
        }
    }
}
