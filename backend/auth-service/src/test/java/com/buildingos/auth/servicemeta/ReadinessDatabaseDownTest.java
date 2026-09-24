package com.buildingos.auth.servicemeta;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;

import static org.assertj.core.api.Assertions.assertThat;

/** Database outage must fail readiness without crashing the process (liveness stays up). */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.datasource.url=jdbc:postgresql://127.0.0.1:1/auth_db",
        "spring.flyway.enabled=false",
        "platform.security.issuer=http://127.0.0.1:1/issuer",
        "platform.security.audience=auth-platform",
        "platform.security.jwk-set-uri=http://127.0.0.1:1/jwks"
})
@ActiveProfiles("test")
class ReadinessDatabaseDownTest {

    private static final HttpClient HTTP = HttpClient.newHttpClient();

    @LocalServerPort
    private int port;

    private HttpResponse<String> get(String path) throws Exception {
        return HTTP.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void readinessReportsDownWhenDatabaseUnreachable() throws Exception {
        assertThat(get("/actuator/health/readiness").statusCode()).isEqualTo(503);
    }

    @Test
    void livenessStaysUpWhenDatabaseUnreachable() throws Exception {
        assertThat(get("/actuator/health/liveness").statusCode()).isEqualTo(200);
    }
}
