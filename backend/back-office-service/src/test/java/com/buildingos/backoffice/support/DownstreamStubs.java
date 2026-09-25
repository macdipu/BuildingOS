package com.buildingos.backoffice.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/** Local-only stand-ins for auth-service {@code /internal/users/{id}} and building-service buildings. Tests only. */
public final class DownstreamStubs implements AutoCloseable {
    private final HttpServer server;
    public final Map<UUID, List<String>> users = new ConcurrentHashMap<>();
    public final Set<UUID> buildings = ConcurrentHashMap.newKeySet();
    public volatile boolean authDown;
    public volatile boolean buildingDown;
    public final List<String> authAuthorizations = new CopyOnWriteArrayList<>();
    public final List<String> buildingAuthorizations = new CopyOnWriteArrayList<>();

    public DownstreamStubs() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/users/", exchange -> {
            authAuthorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            if (authDown) {
                send(exchange, 500, "{\"success\":false,\"code\":\"INTERNAL_ERROR\",\"message\":\"x\"}");
                return;
            }
            UUID id = lastSegment(exchange);
            List<String> roles = id == null ? null : users.get(id);
            if (roles == null) {
                send(exchange, 404, "{\"success\":false,\"code\":\"USER_NOT_FOUND\",\"message\":\"User not found\"}");
                return;
            }
            String json = roles.stream().map(r -> "\"" + r + "\"").collect(Collectors.joining(","));
            send(exchange, 200, "{\"success\":true,\"data\":{\"id\":\"" + id + "\",\"phone\":\"01712345678\","
                    + "\"createdAt\":\"2026-01-01T00:00:00Z\",\"platformRoles\":[" + json + "]},\"meta\":{}}");
        });
        server.createContext("/api/v1/platform/buildings/", exchange -> {
            buildingAuthorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            if (buildingDown) {
                send(exchange, 503, "{\"success\":false,\"code\":\"SERVICE_UNAVAILABLE\",\"message\":\"x\"}");
                return;
            }
            UUID id = lastSegment(exchange);
            if (id == null || !buildings.contains(id)) {
                send(exchange, 404, "{\"success\":false,\"code\":\"BUILDING_NOT_FOUND\",\"message\":\"x\"}");
                return;
            }
            send(exchange, 200, "{\"success\":true,\"data\":{\"id\":\"" + id + "\",\"status\":\"ONBOARDING\"}}");
        });
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void reset() {
        users.clear();
        buildings.clear();
        authDown = false;
        buildingDown = false;
        authAuthorizations.clear();
        buildingAuthorizations.clear();
    }

    private static UUID lastSegment(HttpExchange exchange) {
        String path = exchange.getRequestURI().getPath();
        try {
            return UUID.fromString(path.substring(path.lastIndexOf('/') + 1));
        } catch (IllegalArgumentException notAnId) {
            return null;
        }
    }

    private static void send(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        try (var os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
