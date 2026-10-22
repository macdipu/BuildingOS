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

/**
 * Local-only stand-ins for auth-service's service-token exchange and the D-38 directory lookups in auth-service and
 * building-service. Directory calls succeed only with the service token the exchange issued. Tests only.
 */
public final class DownstreamStubs implements AutoCloseable {
    /** Must match {@code buildingos.service-credential.client-secret} in application-test.yaml. */
    public static final String CLIENT_SECRET = "test-only-back-office-client-secret-0001";
    public static final String SERVICE_TOKEN = "stub-service-token";
    private final HttpServer server;
    public final Map<UUID, List<String>> users = new ConcurrentHashMap<>();
    public final Set<UUID> buildings = ConcurrentHashMap.newKeySet();
    public volatile boolean authDown;
    public volatile boolean buildingDown;
    public volatile boolean rejectClient;
    /** Directory refuses even the service token, e.g. a scope misconfiguration. */
    public volatile boolean denyDirectory;
    /** Token exchange answers 404, as an auth-service without the endpoint would. */
    public volatile boolean tokenEndpointMissing;
    public final List<String> tokenExchanges = new CopyOnWriteArrayList<>();
    public final List<String> authAuthorizations = new CopyOnWriteArrayList<>();
    public final List<String> buildingAuthorizations = new CopyOnWriteArrayList<>();

    public DownstreamStubs() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/service-tokens", exchange -> {
            String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            tokenExchanges.add(body);
            if (authDown) {
                send(exchange, 500, "{\"success\":false,\"code\":\"INTERNAL_ERROR\",\"message\":\"x\"}");
                return;
            }
            if (tokenEndpointMissing) {
                send(exchange, 404, "{\"success\":false,\"code\":\"NOT_FOUND\",\"message\":\"x\"}");
                return;
            }
            if (rejectClient || !body.contains("\"clientId\":\"back-office-service\"")
                    || !body.contains("\"clientSecret\":\"" + CLIENT_SECRET + "\"")) {
                send(exchange, 401, "{\"success\":false,\"code\":\"INVALID_CLIENT\",\"message\":\"x\"}");
                return;
            }
            send(exchange, 200, "{\"success\":true,\"data\":{\"accessToken\":\"" + SERVICE_TOKEN
                    + "\",\"tokenType\":\"Bearer\",\"expiresIn\":300}}");
        });
        server.createContext("/internal/directory/users/", exchange -> {
            authAuthorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            if (denyDirectory || !serviceTokenPresented(exchange)) {
                send(exchange, 403, "{\"success\":false,\"code\":\"ACCESS_DENIED\",\"message\":\"x\"}");
                return;
            }
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
            send(exchange, 200, "{\"success\":true,\"data\":{\"id\":\"" + id + "\",\"platformRoles\":[" + json
                    + "]},\"meta\":{}}");
        });
        server.createContext("/internal/directory/buildings/", exchange -> {
            buildingAuthorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            if (denyDirectory || !serviceTokenPresented(exchange)) {
                send(exchange, 403, "{\"success\":false,\"code\":\"ACCESS_DENIED\",\"message\":\"x\"}");
                return;
            }
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
        rejectClient = false;
        denyDirectory = false;
        tokenEndpointMissing = false;
        tokenExchanges.clear();
        authAuthorizations.clear();
        buildingAuthorizations.clear();
    }

    private static boolean serviceTokenPresented(HttpExchange exchange) {
        return ("Bearer " + SERVICE_TOKEN).equals(exchange.getRequestHeaders().getFirst("Authorization"));
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
