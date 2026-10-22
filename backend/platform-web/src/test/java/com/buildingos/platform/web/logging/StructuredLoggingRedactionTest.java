package com.buildingos.platform.web.logging;

import com.buildingos.platform.web.correlation.CorrelationFilter;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * STORY-004 AC 1 and AC 3 end to end: with the services' logging settings, Boot's logstash console format emits one
 * JSON object per line, carries {@code service} and {@code correlationId}, and the spring.factories-registered
 * {@link RedactingJsonMembersCustomizer} masks message, MDC and stack-trace text.
 */
@ExtendWith(OutputCaptureExtension.class)
class StructuredLoggingRedactionTest {

    private static final JsonMapper JSON = JsonMapper.builder().build();

    @Configuration(proxyBeanMethods = false)
    static class Empty {
    }

    @Test
    void emitsRedactedJsonLinesWithServiceAndCorrelationId(CapturedOutput output) {
        String correlationId = "6f1c2d3e-4b5a-4c6d-8e7f-0123456789ab";
        String jwt = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1In0.c2ln";
        try (ConfigurableApplicationContext ignored = new SpringApplicationBuilder(Empty.class)
                .web(WebApplicationType.NONE)
                .properties("spring.application.name=logging-probe",
                        "logging.structured.format.console=logstash",
                        "logging.structured.json.add.service=${spring.application.name}",
                        "logging.structured.json.rename.logger_name=logger")
                .run()) {
            var log = LoggerFactory.getLogger("probe");
            try (MDC.MDCCloseable c = MDC.putCloseable(CorrelationFilter.MDC_KEY, correlationId);
                 MDC.MDCCloseable p = MDC.putCloseable("password", "hunter2")) {
                log.info("otp_sent phone=01712345678 otp=123456 auth=Bearer {}", jwt);
                log.warn("presign failed", new IllegalStateException(
                        "https://s3.example/b/k?X-Amz-Signature=abc for 01912345601"));
            }
        }

        List<JsonNode> lines = output.getOut().lines().filter(l -> l.startsWith("{")).map(JSON::readTree).toList();
        assertThat(lines).isNotEmpty().allSatisfy(line -> {
            assertThat(line.path("service").asString()).isEqualTo("logging-probe");
            assertThat(line.has("logger")).isTrue();
            assertThat(line.has("level")).isTrue();
            assertThat(line.has("@timestamp")).isTrue();
        });

        JsonNode info = lines.stream().filter(l -> l.path("message").asString().startsWith("otp_sent")).findFirst()
                .orElseThrow();
        assertThat(info.path("correlationId").asString()).isEqualTo(correlationId);
        assertThat(info.path("message").asString()).isEqualTo("otp_sent phone=01*******78 otp=[redacted] auth=Bearer [jwt]");
        assertThat(info.path("password").asString()).isEqualTo(LogRedactor.REDACTED);

        JsonNode warn = lines.stream().filter(l -> "presign failed".equals(l.path("message").asString())).findFirst()
                .orElseThrow();
        assertThat(warn.path("stack_trace").asString())
                .contains("https://s3.example/b/k for 01*******01")
                .doesNotContain("X-Amz-Signature", "01912345601");
        assertThat(output.getOut()).doesNotContain("01712345678", jwt, "hunter2");
    }
}
