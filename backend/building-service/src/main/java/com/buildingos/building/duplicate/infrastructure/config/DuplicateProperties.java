package com.buildingos.building.duplicate.infrastructure.config;

import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("buildingos.duplicates")
public record DuplicateProperties(double radiusMeters, List<String> ignoredTokens) {
    public DuplicateProperties {
        ignoredTokens = ignoredTokens == null ? List.of() : List.copyOf(ignoredTokens);
    }
}
