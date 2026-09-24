package com.buildingos.gateway.support;

import com.sun.net.httpserver.HttpServer;
import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;

/** Local-only stub for a downstream service's /internal/platform/info endpoint, for gateway routing tests. */
public final class StubDownstream implements AutoCloseable {
    private final HttpServer server;
    public final String baseUrl;
    private final AtomicReference<String> lastCorrelationHeader = new AtomicReference<>();
    private final AtomicReference<String> lastAuthorization = new AtomicReference<>();
    private final AtomicReference<byte[]> lastBody = new AtomicReference<>(new byte[0]);
    private final AtomicReference<String> lastContentType = new AtomicReference<>();

    public StubDownstream(String service) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/platform/info", exchange -> {
            lastCorrelationHeader.set(exchange.getRequestHeaders().getFirst("X-Correlation-Id"));
            byte[] body = ("{\"success\":true,\"data\":{\"service\":\"" + service + "\"},\"meta\":{},\"traceId\":\"stub\"}")
                    .getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/api/v1/auth/otp/start", exchange -> {
            lastCorrelationHeader.set(exchange.getRequestHeaders().getFirst("X-Correlation-Id"));
            byte[] body = "{\"success\":true,\"data\":{\"stub\":\"otp-start\"},\"meta\":{},\"traceId\":\"stub\"}".getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.createContext("/api/v1/", exchange -> {
            lastCorrelationHeader.set(exchange.getRequestHeaders().getFirst("X-Correlation-Id"));
            lastAuthorization.set(exchange.getRequestHeaders().getFirst("Authorization"));
            lastContentType.set(exchange.getRequestHeaders().getFirst("Content-Type"));
            lastBody.set(exchange.getRequestBody().readAllBytes());
            byte[] body = ("{\"success\":true,\"data\":{\"service\":\"" + service + "\",\"method\":\""
                    + exchange.getRequestMethod() + "\",\"path\":\"" + exchange.getRequestURI().getPath()
                    + "\"},\"meta\":{},\"traceId\":\"stub\"}").getBytes();
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
        baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public String lastAuthorization() {
        return lastAuthorization.get();
    }

    public byte[] lastBody() {
        return lastBody.get();
    }

    public String lastContentType() {
        return lastContentType.get();
    }

    public String lastCorrelationHeader() {
        return lastCorrelationHeader.get();
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
