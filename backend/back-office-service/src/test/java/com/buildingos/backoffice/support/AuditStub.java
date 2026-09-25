package com.buildingos.backoffice.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Local-only stand-in for one service's {@code GET /internal/audit}. Tests only. */
public final class AuditStub implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    private final String source;
    public volatile int status = 200;
    public volatile long delayMs;
    /** InternalAuditEvent JSON objects returned as {@code data}, in the order given. */
    public final List<String> events = new CopyOnWriteArrayList<>();
    public final List<String> queries = new CopyOnWriteArrayList<>();
    public final List<String> authorizations = new CopyOnWriteArrayList<>();

    public AuditStub(String source) throws IOException {
        this.source = source;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/internal/audit", exchange -> {
            queries.add(String.valueOf(exchange.getRequestURI().getRawQuery()));
            authorizations.add(exchange.getRequestHeaders().getFirst("Authorization"));
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            String body = status == 200
                    ? "{\"success\":true,\"data\":[" + String.join(",", events) + "],\"meta\":{}}"
                    : "{\"success\":false,\"code\":\"SERVICE_UNAVAILABLE\",\"message\":\"x\"}";
            byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, bytes.length);
            try (var os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        });
        server.start();
    }

    /** One InternalAuditEvent row of this stub's source. */
    public String event(String id, String occurredAt, String action) {
        return "{\"id\":\"" + id + "\",\"source\":\"" + source + "\",\"occurredAt\":\"" + occurredAt
                + "\",\"actorUserId\":null,\"action\":\"" + action + "\",\"entityType\":\"X\",\"entityId\":\"e-" + id
                + "\",\"buildingId\":null,\"reason\":null}";
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void reset() {
        status = 200;
        delayMs = 0;
        events.clear();
        queries.clear();
        authorizations.clear();
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
