package com.buildingos.building.unit.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code buildingos.units.batch.*}: technical bulk safeguards (TECH-SPEC-F4), defaults 500 rows and 1 MiB. */
@ConfigurationProperties("buildingos.units.batch")
public record UnitBatchProperties(Integer maxRows, Long maxSheetBytes) {
    public UnitBatchProperties {
        maxRows = maxRows == null ? 500 : maxRows;
        maxSheetBytes = maxSheetBytes == null ? 1_048_576L : maxSheetBytes;
    }
}
