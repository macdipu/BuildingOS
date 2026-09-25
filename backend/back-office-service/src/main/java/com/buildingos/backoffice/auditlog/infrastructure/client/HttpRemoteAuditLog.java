package com.buildingos.backoffice.auditlog.infrastructure.client;

import com.buildingos.backoffice.auditlog.application.port.out.RemoteAuditLog;
import com.buildingos.backoffice.auditlog.domain.model.AuditEvent;
import com.buildingos.backoffice.auditlog.domain.model.AuditFilter;
import com.buildingos.backoffice.auditlog.domain.model.AuditSource;
import com.buildingos.backoffice.shared.infrastructure.client.BearerTokenRelay;
import com.buildingos.backoffice.shared.infrastructure.client.ServiceClientProperties;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriBuilder;
import tools.jackson.databind.JsonNode;

/**
 * {@code GET <service>/internal/audit} on auth-, building- and subscription-service with the caller's relayed token;
 * each service checks SUPER_ADMIN itself. Connect and read timeouts are the audit timeout. Any failure propagates.
 */
public class HttpRemoteAuditLog implements RemoteAuditLog {
    private static final String PATH = "/internal/audit";
    private final Map<AuditSource, RestClient> clients = new EnumMap<>(AuditSource.class);

    public HttpRemoteAuditLog(ServiceClientProperties services, Duration timeout) {
        clients.put(AuditSource.AUTH_SERVICE, client(services.authUrl(), timeout));
        clients.put(AuditSource.BUILDING_SERVICE, client(services.buildingUrl(), timeout));
        clients.put(AuditSource.SUBSCRIPTION_SERVICE, client(services.subscriptionUrl(), timeout));
    }

    private static RestClient client(String baseUrl, Duration timeout) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).requestInterceptor(new BearerTokenRelay())
                .build();
    }

    @Override
    public List<AuditEvent> fetch(AuditSource source, AuditFilter filter) {
        RestClient client = clients.get(source);
        if (client == null) {
            throw new IllegalArgumentException("no remote audit endpoint for " + source.wireName());
        }
        JsonNode body = client.get().uri(builder -> uri(builder, filter)).retrieve().body(JsonNode.class);
        JsonNode data = body == null ? null : body.path("data");
        if (data == null || !data.isArray()) {
            throw new IllegalStateException(source.wireName() + " returned no audit data array");
        }
        List<AuditEvent> events = new ArrayList<>(data.size());
        for (JsonNode item : data) {
            events.add(new AuditEvent(UUID.fromString(item.path("id").asString()), source,
                    Instant.parse(item.path("occurredAt").asString()), uuid(item, "actorUserId"),
                    item.path("action").asString(), item.path("entityType").asString(),
                    item.path("entityId").asString(), uuid(item, "buildingId"), text(item, "reason"),
                    text(item, "role")));
        }
        return events;
    }

    private static java.net.URI uri(UriBuilder builder, AuditFilter filter) {
        builder.path(PATH).queryParam("limit", filter.limit());
        if (filter.since() != null) {
            builder.queryParam("since", filter.since().toString());
        }
        if (filter.until() != null) {
            builder.queryParam("until", filter.until().toString());
        }
        if (filter.entityType() != null) {
            builder.queryParam("entityType", filter.entityType());
        }
        if (filter.actorUserId() != null) {
            builder.queryParam("actorUserId", filter.actorUserId());
        }
        return builder.build();
    }

    private static String text(JsonNode item, String field) {
        JsonNode value = item.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    private static UUID uuid(JsonNode item, String field) {
        String value = text(item, field);
        return value == null ? null : UUID.fromString(value);
    }
}
