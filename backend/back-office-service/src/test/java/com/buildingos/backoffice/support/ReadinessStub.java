package com.buildingos.backoffice.support;

import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Local-only stand-in for one service's {@code /actuator/health/readiness}. Tests only. */
public final class ReadinessStub implements AutoCloseable {
    private final HttpServer server;
    private final ExecutorService executor = Executors.newCachedThreadPool();
    public volatile int status = 200;
    public volatile long delayMs;

    public ReadinessStub() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.setExecutor(executor);
        server.createContext("/actuator/health/readiness", exchange -> {
            if (delayMs > 0) {
                try {
                    Thread.sleep(delayMs);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            byte[] body = ("{\"status\":\"" + (status == 200 ? "UP" : "DOWN")
                    + "\",\"components\":{\"db\":{\"status\":\"SECRET-DETAIL\"}}}").getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(status, body.length);
            try (var os = exchange.getResponseBody()) {
                os.write(body);
            } catch (IOException clientGaveUp) {
                // the prober timed out and closed the connection
            }
        });
        server.start();
    }

    public String url() {
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    public void reset() {
        status = 200;
        delayMs = 0;
    }

    @Override
    public void close() {
        server.stop(0);
        executor.shutdownNow();
    }
}
