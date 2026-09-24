package com.buildingos.building.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicReference;

/** Local-only stand-ins for auth-service provisioning and subscription-service fee status. Tests only. */
public final class DownstreamStubs implements AutoCloseable {
    public record Reply(int status, String body) {}

    private final HttpServer server;
    public final AtomicReference<Reply> feeReply = new AtomicReference<>(fee("SETTLED"));
    public final AtomicReference<Reply> provisionReply = new AtomicReference<>();
    public final UUID provisionedUserId = UUID.randomUUID();
    public final List<String> feeAuthorizations = new CopyOnWriteArrayList<>();
    public final List<String> provisionBodies = new CopyOnWriteArrayList<>();
    public final List<String> provisionAuthorizations = new CopyOnWriteArrayList<>();

    public DownstreamStubs() throws IOException {
        provisionReply.set(new Reply(200, "{\"success\":true,\"data\":{\"userId\":\"" + provisionedUserId
                + "\",\"phone\":\"01812345678\"}}"));
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/platform/fees/BUILDING_CREATION/status", exchange -> {
            feeAuthorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            send(exchange, feeReply.get());
        });
        server.createContext("/internal/users/provision", exchange -> {
            provisionAuthorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            provisionBodies.add(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            send(exchange, provisionReply.get());
        });
        server.start();
    }

    public static Reply fee(String status) {
        return new Reply(200, "{\"success\":true,\"data\":{\"status\":\"" + status + "\"}}");
    }

    public static Reply feeNotConfigured() {
        return new Reply(409, "{\"success\":false,\"code\":\"FEE_NOT_CONFIGURED\",\"message\":\"x\"}");
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void reset() {
        feeReply.set(fee("SETTLED"));
        provisionReply.set(new Reply(200, "{\"success\":true,\"data\":{\"userId\":\"" + provisionedUserId
                + "\",\"phone\":\"01812345678\"}}"));
        feeAuthorizations.clear();
        provisionBodies.clear();
        provisionAuthorizations.clear();
    }

    private static void send(HttpExchange exchange, Reply reply) throws IOException {
        byte[] body = reply.body().getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(reply.status(), body.length);
        try (var os = exchange.getResponseBody()) {
            os.write(body);
        }
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
