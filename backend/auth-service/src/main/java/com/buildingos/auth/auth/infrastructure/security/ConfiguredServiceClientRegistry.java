package com.buildingos.auth.auth.infrastructure.security;

import com.buildingos.auth.auth.application.port.out.ServiceClientRegistry;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Service clients from configuration (D-38). Secrets are kept only as SHA-256 digests and compared in constant
 * time; a client with a blank secret is disabled.
 */
public final class ConfiguredServiceClientRegistry implements ServiceClientRegistry {
    static final int MIN_SECRET_LENGTH = 32;
    private final Map<String, Client> clients;

    public record ClientConfig(String secret, List<String> scopes) {}

    private record Client(byte[] secretDigest, Set<String> scopes) {}

    public ConfiguredServiceClientRegistry(Map<String, ClientConfig> configured) {
        var enabled = new HashMap<String, Client>();
        (configured == null ? Map.<String, ClientConfig>of() : configured).forEach((id, config) -> {
            if (config == null || config.secret() == null || config.secret().isBlank()) return;
            if (config.secret().length() < MIN_SECRET_LENGTH) {
                throw new IllegalArgumentException("Service client secret is too short");
            }
            if (config.scopes() == null || config.scopes().isEmpty()) {
                throw new IllegalArgumentException("Service client needs at least one scope");
            }
            enabled.put(id, new Client(digest(config.secret()), Set.copyOf(config.scopes())));
        });
        this.clients = Map.copyOf(enabled);
    }

    @Override
    public Optional<Set<String>> authenticate(String clientId, String clientSecret) {
        if (clientId == null || clientSecret == null) return Optional.empty();
        Client client = clients.get(clientId);
        // Digest even for an unknown id so the response time does not reveal which ids exist.
        byte[] presented = digest(clientSecret);
        byte[] expected = client == null ? new byte[presented.length] : client.secretDigest();
        boolean match = MessageDigest.isEqual(presented, expected);
        return client != null && match ? Optional.of(client.scopes()) : Optional.empty();
    }

    private static byte[] digest(String secret) {
        try {
            return MessageDigest.getInstance("SHA-256").digest(secret.getBytes(StandardCharsets.UTF_8));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
