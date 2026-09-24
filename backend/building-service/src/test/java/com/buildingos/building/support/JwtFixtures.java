package com.buildingos.building.support;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

/** Local-only fixture JWKS server and token signer for security integration tests. Never used outside tests. */
public final class JwtFixtures implements AutoCloseable {
    private final RSAKey rsaJwk;
    private final HttpServer server;
    public final String jwkSetUri;

    public JwtFixtures() throws Exception {
        rsaJwk = generateKey();
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/jwks", exchange -> {
            byte[] body = ("{\"keys\":[" + rsaJwk.toPublicJWK().toJSONString() + "]}").getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        jwkSetUri = "http://127.0.0.1:" + server.getAddress().getPort() + "/jwks";
    }

    public String issuer() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public String token(String issuer, String audience, Instant expiry) throws Exception {
        return sign(rsaJwk, issuer, audience, expiry);
    }

    /** Token shaped like auth-service's: {@code sub} = user id plus {@code platform_roles}. */
    public String userToken(String audience, UUID userId, java.util.List<String> platformRoles) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer())
                .audience(audience)
                .subject(userId.toString())
                .claim("platform_roles", platformRoles)
                .issueTime(Date.from(Instant.now().minusSeconds(10)))
                .expirationTime(Date.from(Instant.now().plusSeconds(300)))
                .build();
        SignedJWT jwt = new SignedJWT(new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(rsaJwk.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(rsaJwk));
        return jwt.serialize();
    }

    public String tokenSignedByOtherKey(String issuer, String audience) throws Exception {
        return sign(generateKey(), issuer, audience, Instant.now().plusSeconds(300));
    }

    private static String sign(RSAKey signingKey, String issuer, String audience, Instant expiry) throws Exception {
        JWTClaimsSet claims = new JWTClaimsSet.Builder()
                .issuer(issuer)
                .audience(audience)
                .subject("fixture-subject")
                .issueTime(Date.from(Instant.now().minusSeconds(10)))
                .expirationTime(Date.from(expiry))
                .build();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(signingKey.getKeyID()).build(), claims);
        jwt.sign(new RSASSASigner(signingKey));
        return jwt.serialize();
    }

    private static RSAKey generateKey() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        var pair = generator.generateKeyPair();
        return new RSAKey.Builder((RSAPublicKey) pair.getPublic())
                .privateKey((RSAPrivateKey) pair.getPrivate())
                .keyID(UUID.randomUUID().toString())
                .build();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
