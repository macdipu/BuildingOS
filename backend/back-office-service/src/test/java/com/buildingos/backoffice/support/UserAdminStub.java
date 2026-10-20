package com.buildingos.backoffice.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

/** Local-only stand-in for auth-service {@code /internal/users*} (F6-T2). Tests only. */
public final class UserAdminStub implements AutoCloseable {
    private final HttpServer server;
    public final Map<UUID, TreeSet<String>> users = new ConcurrentHashMap<>();
    public volatile int forcedStatus;
    public final List<String> requests = new CopyOnWriteArrayList<>();
    public final List<String> authorizations = new CopyOnWriteArrayList<>();

    public UserAdminStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/users", this::handle);
        server.start();
    }

    private void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String query = ex.getRequestURI().getRawQuery();
        requests.add(ex.getRequestMethod() + " " + path + (query == null ? "" : "?" + query));
        authorizations.add(String.valueOf(ex.getRequestHeaders().getFirst("Authorization")));
        if (forcedStatus != 0) {
            send(ex, forcedStatus, "{\"success\":false,\"code\":\"X\",\"message\":\"forced\"}");
            return;
        }
        String[] parts = path.substring("/internal/users".length()).split("/");
        // "" | "/{id}" | "/{id}/platform-roles" | "/{id}/platform-roles/{role}"
        if (parts.length <= 1) {
            var all = users.entrySet().stream().map(e -> json(e.getKey(), e.getValue())).collect(Collectors.toList());
            send(ex, 200, "{\"success\":true,\"data\":[" + String.join(",", all) + "],\"meta\":{\"page\":0,\"size\":20,\"total\":"
                    + all.size() + "}}");
            return;
        }
        UUID id = UUID.fromString(parts[1]);
        TreeSet<String> roles = users.get(id);
        if (roles == null) {
            send(ex, 404, "{\"success\":false,\"code\":\"USER_NOT_FOUND\",\"message\":\"User not found\"}");
            return;
        }
        if (parts.length == 3 && ex.getRequestMethod().equals("POST")) {
            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            roles.add(body.replaceAll(".*\"role\"\\s*:\\s*\"([A-Z_]+)\".*", "$1"));
        } else if (parts.length == 4 && ex.getRequestMethod().equals("DELETE")) {
            roles.remove(parts[3]);
            ex.sendResponseHeaders(204, -1);
            ex.close();
            return;
        }
        send(ex, 200, "{\"success\":true,\"data\":" + json(id, roles) + ",\"meta\":{}}");
    }

    private static String json(UUID id, TreeSet<String> roles) {
        String list = roles.stream().map(r -> "\"" + r + "\"").collect(Collectors.joining(","));
        return "{\"id\":\"" + id + "\",\"phone\":\"01712345678\",\"createdAt\":\"2026-01-01T00:00:00Z\","
                + "\"platformRoles\":[" + list + "]}";
    }

    private static void send(HttpExchange ex, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        ex.getResponseHeaders().add("Content-Type", "application/json");
        ex.sendResponseHeaders(status, bytes.length);
        try (var os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void reset() {
        users.clear();
        forcedStatus = 0;
        requests.clear();
        authorizations.clear();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
