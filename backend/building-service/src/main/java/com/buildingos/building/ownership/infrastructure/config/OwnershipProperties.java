package com.buildingos.building.ownership.infrastructure.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * {@code buildingos.ownership.*}: calendar for "today" effective dates (UO-D02), default Asia/Dhaka, and the
 * technical cap on active documents per transfer, default 10.
 */
@ConfigurationProperties("buildingos.ownership")
public record OwnershipProperties(ZoneId zone, Integer maxDocumentsPerTransfer) {
    public OwnershipProperties {
        zone = zone == null ? ZoneId.of("Asia/Dhaka") : zone;
        maxDocumentsPerTransfer = maxDocumentsPerTransfer == null ? 10 : maxDocumentsPerTransfer;
    }
}
