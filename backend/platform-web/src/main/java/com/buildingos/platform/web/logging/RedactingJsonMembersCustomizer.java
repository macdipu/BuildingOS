package com.buildingos.platform.web.logging;

import org.springframework.boot.json.JsonWriter;
import org.springframework.boot.logging.structured.StructuredLoggingJsonMembersCustomizer;

/**
 * Runs {@link LogRedactor} over every string member (message, stack trace, MDC and key-value pairs) of a
 * structured log line. Registered in {@code META-INF/spring.factories}, so every service that depends on
 * platform-web and enables {@code logging.structured.format.console} gets it without further configuration.
 */
public final class RedactingJsonMembersCustomizer implements StructuredLoggingJsonMembersCustomizer<Object> {

    @Override
    public void customize(JsonWriter.Members<Object> members) {
        JsonWriter.ValueProcessor<String> redact =
                (path, value) -> LogRedactor.redact(path == null ? null : path.name(), value);
        members.applyingValueProcessor(redact.whenInstanceOf(String.class));
    }
}
