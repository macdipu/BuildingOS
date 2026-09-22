package com.buildingos.identity.auth.infrastructure.security;

import com.buildingos.identity.auth.application.port.out.TokenIssuer;
import com.buildingos.identity.auth.domain.PlatformRole;
import com.buildingos.platform.web.SecuritySettings;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Signs access tokens with an in-process RSA keypair and publishes the public half as a
 * JWKS (see {@link JwkSetController}). Local/test only — BOS-001's shared security config
 * already rejects any HTTP issuer/JWKS URI outside the local/test profile, so this issuer
 * is meant to be pointed at from JWT_ISSUER/JWT_JWK_SET_URI in exactly that setup. A real
 * deployment configures an external, HTTPS-published issuer instead (see
 * docs/LOCAL_DEVELOPMENT.md); this class must not run outside local/test.
 */
@Component
@Profile({"local", "test"})
public final class LocalRsaJwtIssuer implements TokenIssuer {
    private static final Duration ACCESS_TOKEN_TTL = Duration.ofMinutes(15);

    private final RSAKey signingKey;
    private final SecuritySettings settings;
    private final Clock clock;

    public LocalRsaJwtIssuer(SecuritySettings settings, Clock clock) {
        this.settings = settings;
        this.clock = clock;
        this.signingKey = generateKey();
    }

    RSAKey publicJwk() {
        return signingKey.toPublicJWK();
    }

    @Override
    public IssuedToken issue(UUID userId, String phone, Set<PlatformRole> platformRoles) {
        Instant now = clock.instant();
        Instant expiry = now.plus(ACCESS_TOKEN_TTL);
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
            return new IssuedToken(jwt.serialize(), ACCESS_TOKEN_TTL.toSeconds());
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
