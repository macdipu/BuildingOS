package com.buildingos.backoffice.platformuser.infrastructure.client;

import com.buildingos.backoffice.platformuser.application.port.out.PlatformUserAdmin;
import com.buildingos.backoffice.platformuser.domain.model.PlatformUser;
import com.buildingos.backoffice.shared.application.DependencyUnavailableException;
import com.buildingos.backoffice.shared.application.NotPermittedException;
import com.buildingos.backoffice.shared.application.Page;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** auth-service {@code /internal/users*} (F6-T2) with the caller's relayed token. */
@Component
public class AuthPlatformUserAdmin implements PlatformUserAdmin {
    private static final String DEPENDENCY = "auth-service";
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private final RestClient client;

    public AuthPlatformUserAdmin(@Qualifier("authServiceClient") RestClient client) {
        this.client = client;
    }

    @Override
    public Page<PlatformUser> list(String query, String role, int page, int size) {
        JsonNode body = call(() -> client.get().uri(uri -> {
            uri.path("/internal/users").queryParam("page", page).queryParam("size", size);
            if (query != null) {
                uri.queryParam("query", query);
            }
            if (role != null) {
                uri.queryParam("role", role);
            }
            return uri.build();
        }).retrieve().body(JsonNode.class));
        JsonNode data = body == null ? null : body.path("data");
        JsonNode meta = body == null ? null : body.path("meta");
        if (data == null || !data.isArray() || meta == null || !meta.path("total").isNumber()) {
            throw malformed();
        }
        List<PlatformUser> users = new ArrayList<>(data.size());
        for (JsonNode item : data) {
            users.add(user(item));
        }
        return new Page<>(users, page, size, meta.path("total").asLong());
    }

    @Override
    public Optional<PlatformUser> find(UUID userId) {
        try {
            return Optional.of(user(single(call(() -> client.get().uri("/internal/users/{id}", userId).retrieve()
                    .body(JsonNode.class)))));
        } catch (HttpClientErrorException.NotFound unknown) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<PlatformUser> assign(UUID userId, String role) {
        try {
            return Optional.of(user(single(call(() -> client.post().uri("/internal/users/{id}/platform-roles", userId)
                    .body(java.util.Map.of("role", role)).retrieve().body(JsonNode.class)))));
        } catch (HttpClientErrorException.NotFound unknown) {
            return Optional.empty();
        }
    }

    @Override
    public Optional<PlatformUser> revoke(UUID userId, String role) {
        try {
            call(() -> client.delete().uri("/internal/users/{id}/platform-roles/{role}", userId, role).retrieve()
                    .toBodilessEntity());
        } catch (HttpClientErrorException.NotFound unknown) {
            return Optional.empty();
        }
        return find(userId);
    }

    /** Maps auth-service answers: 403 -> not permitted, 404/400 propagate for the caller, anything else = down. */
    private static <T> T call(java.util.function.Supplier<T> request) {
        try {
            return request.get();
        } catch (HttpClientErrorException.NotFound notFound) {
            throw notFound;
        } catch (HttpClientErrorException denied) {
            HttpStatusCode status = denied.getStatusCode();
            if (status.value() == 403) {
                throw new NotPermittedException();
            }
            if (status.value() == 400) {
                throw new IllegalArgumentException(message(denied));
            }
            throw new DependencyUnavailableException(DEPENDENCY, denied);
        } catch (RestClientException failed) {
            throw new DependencyUnavailableException(DEPENDENCY, failed);
        }
    }

    private static String message(HttpClientErrorException failure) {
        try {
            String text = JSON.readTree(failure.getResponseBodyAsString()).path("message").asString();
            return text == null || text.isBlank() ? "Request failed validation" : text;
        } catch (RuntimeException unreadable) {
            return "Request failed validation";
        }
    }

    private static JsonNode single(JsonNode body) {
        JsonNode data = body == null ? null : body.path("data");
        if (data == null || !data.isObject()) {
            throw malformed();
        }
        return data;
    }

    private static PlatformUser user(JsonNode item) {
        JsonNode roles = item.path("platformRoles");
        if (!roles.isArray() || item.path("id").asString().isEmpty()) {
            throw malformed();
        }
        List<String> names = new ArrayList<>(roles.size());
        for (JsonNode role : roles) {
            names.add(role.asString());
        }
        return new PlatformUser(UUID.fromString(item.path("id").asString()), item.path("phone").asString(),
                Instant.parse(item.path("createdAt").asString()), names);
    }

    private static DependencyUnavailableException malformed() {
        return new DependencyUnavailableException(DEPENDENCY, new IllegalStateException("unexpected auth-service reply"));
    }
}
