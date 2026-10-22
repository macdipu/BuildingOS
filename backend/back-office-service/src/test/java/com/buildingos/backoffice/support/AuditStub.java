package com.buildingos.backoffice.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Local-only stand-in for one service's {@code GET /internal/audit}. Tests only. Like the real sources it returns
 * {@code events} newest first ({@code occurredAt}, then id), honouring {@code until}/{@code untilId} and {@code limit}.
 */
public final class AuditStub implements AutoCloseable {
    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final Comparator<JsonNode> NEWEST_FIRST = Comparator
            .comparing((JsonNode e) -> Instant.parse(e.path("occurredAt").asString()))
            .thenComparing(e -> e.path("id").asString())
            .reversed();
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
                    ? "{\"success\":true,\"data\":[" + String.join(",", page(exchange.getRequestURI().getRawQuery()))
                            + "],\"meta\":{}}"
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

    private List<String> page(String rawQuery) {
        Map<String, String> params = new HashMap<>();
        if (rawQuery != null) {
            for (String pair : rawQuery.split("&")) {
                int eq = pair.indexOf('=');
                if (eq > 0) {
                    params.put(pair.substring(0, eq),
                            URLDecoder.decode(pair.substring(eq + 1), StandardCharsets.UTF_8));
                }
            }
        }
        Instant until = params.containsKey("until") ? Instant.parse(params.get("until")) : null;
        String untilId = params.get("untilId");
        int limit = params.containsKey("limit") ? Integer.parseInt(params.get("limit")) : Integer.MAX_VALUE;
        return events.stream().map(JSON::readTree).sorted(NEWEST_FIRST).filter(e -> {
            if (until == null) {
                return true;
            }
            int byTime = Instant.parse(e.path("occurredAt").asString()).compareTo(until);
            return byTime < 0 || byTime == 0 && untilId != null && e.path("id").asString().compareTo(untilId) < 0;
        }).limit(limit).map(JsonNode::toString).toList();
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
