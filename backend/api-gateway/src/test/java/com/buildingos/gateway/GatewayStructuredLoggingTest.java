package com.buildingos.gateway;

import com.buildingos.gateway.support.JwtFixtures;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * STORY-004 AC 1: with the service's own logging settings (the test profile's plain-text override switched back to
 * logstash) every console line is a JSON object naming the service, and the request log line carries the
 * {@code X-Correlation-Id} as {@code correlationId}.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "logging.structured.format.console=logstash")
@ActiveProfiles("test")
@ExtendWith(OutputCaptureExtension.class)
class GatewayStructuredLoggingTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();
    private static final HttpClient HTTP = HttpClient.newHttpClient();
    private static final String CORRELATION_ID = "0b6f3c1a-2d4e-4f50-8a7b-9c0d1e2f3a4b";
    private static JwtFixtures fixtures;

    @LocalServerPort
    private int port;

    @BeforeAll
    static void startFixtures() throws Exception {
        fixtures = new JwtFixtures();
    }

    @AfterAll
    static void stopFixtures() {
        fixtures.close();
    }

    @DynamicPropertySource
    static void dynamicProps(DynamicPropertyRegistry registry) {
        registry.add("platform.security.issuer", fixtures::issuer);
        registry.add("platform.security.audience", () -> "gateway-platform");
        registry.add("platform.security.jwk-set-uri", () -> fixtures.jwkSetUri);
        registry.add("AUTH_SERVICE_URL", () -> "http://127.0.0.1:1");
        registry.add("BUILDING_SERVICE_URL", () -> "http://127.0.0.1:1");
        registry.add("SUBSCRIPTION_SERVICE_URL", () -> "http://127.0.0.1:1");
        registry.add("BACK_OFFICE_SERVICE_URL", () -> "http://127.0.0.1:1");
    }

    @Test
    void requestLogIsJsonWithServiceAndCorrelationId(CapturedOutput output) throws Exception {
        var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + "/api/v1/platform/auth"))
                .header("X-Correlation-Id", CORRELATION_ID).GET().build();
        var response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).isEqualTo(401);
        assertThat(response.headers().firstValue("X-Correlation-Id")).contains(CORRELATION_ID);

        List<JsonNode> requestLines = output.getOut().lines()
                .filter(line -> line.contains("http_request") && line.contains(CORRELATION_ID))
                .map(JSON::readTree).toList();
        assertThat(requestLines).singleElement().satisfies(line -> {
            assertThat(line.path("service").asString()).isEqualTo("api-gateway");
            assertThat(line.path("correlationId").asString()).isEqualTo(CORRELATION_ID);
            assertThat(line.path("message").asString()).isEqualTo("http_request method=GET status=401");
            assertThat(line.path("logger").asString()).endsWith("CorrelationFilter");
            assertThat(line.path("level").asString()).isEqualTo("INFO");
        });
    }
}
