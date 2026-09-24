package com.buildingos.building.contract;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.Reader;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.yaml.snakeyaml.Yaml;

/**
 * Every building-service endpoint is published in contracts/openapi/platform.yaml with the same path and method, so
 * the gateway/mobile contract cannot silently drift from the implementation (F4-T6).
 */
class OpenApiCoverageTest {
    private static final Path SPEC = Path.of("..", "..", "contracts", "openapi", "platform.yaml");

    @Test
    void everyControllerOperationIsInTheOpenApiContract() throws Exception {
        Set<String> published = published();
        Set<String> implemented = implemented();

        assertThat(implemented).isNotEmpty();
        assertThat(published).containsAll(implemented);
    }

    @SuppressWarnings("unchecked")
    private static Set<String> published() throws Exception {
        Set<String> operations = new TreeSet<>();
        try (Reader reader = Files.newBufferedReader(SPEC)) {
            Map<String, Object> spec = new Yaml().load(reader);
            ((Map<String, Map<String, Object>>) spec.get("paths")).forEach((path, methods) ->
                    methods.keySet().forEach(method -> operations.add(key(method, path))));
        }
        return operations;
    }

    private static Set<String> implemented() throws Exception {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(RestController.class));
        Set<String> operations = new TreeSet<>();
        for (var candidate : scanner.findCandidateComponents("com.buildingos.building")) {
            Class<?> controller = Class.forName(candidate.getBeanClassName());
            RequestMapping base = AnnotatedElementUtils.findMergedAnnotation(controller, RequestMapping.class);
            String prefix = base == null || base.path().length == 0 ? "" : base.path()[0];
            for (Method method : controller.getDeclaredMethods()) {
                RequestMapping mapping = AnnotatedElementUtils.findMergedAnnotation(method, RequestMapping.class);
                if (mapping == null) {
                    continue;
                }
                String path = prefix + (mapping.path().length == 0 ? "" : mapping.path()[0]);
                for (var verb : mapping.method()) {
                    operations.add(key(verb.name(), path));
                }
            }
        }
        return operations;
    }

    private static String key(String method, String path) {
        return method.toUpperCase() + " " + path.replaceAll("\\{[^}]+}", "{}");
    }
}
