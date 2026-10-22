package com.buildingos.gateway;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

/** STORY-004 AC 1: this service logs logstash JSON with its service name; the test profile keeps plain text. */
class StructuredLoggingConfigTest {

    @Test
    void consoleLogsAreLogstashJsonOutsideTheTestProfile() throws Exception {
        List<PropertySource<?>> documents = new YamlPropertySourceLoader()
                .load("application", new ClassPathResource("application.yaml"));
        PropertySource<?> main = documents.get(0);
        assertThat(main.getProperty("logging.structured.format.console")).hasToString("logstash");
        assertThat(main.getProperty("logging.structured.json.add.service")).hasToString("${spring.application.name}");
        assertThat(main.getProperty("logging.structured.json.rename.logger_name")).hasToString("logger");
        assertThat(main.getProperty("spring.application.name")).hasToString("api-gateway");

        PropertySource<?> test = documents.stream()
                .filter(d -> "test".equals(String.valueOf(d.getProperty("spring.config.activate.on-profile"))))
                .findFirst().orElseThrow();
        assertThat(test.getProperty("logging.structured.format.console")).hasToString("");
    }
}
