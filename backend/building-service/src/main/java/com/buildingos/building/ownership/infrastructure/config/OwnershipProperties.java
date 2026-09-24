package com.buildingos.building.ownership.infrastructure.config;

import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code buildingos.ownership.zone}: calendar for "today" effective dates (UO-D02), default Asia/Dhaka. */
@ConfigurationProperties("buildingos.ownership")
public record OwnershipProperties(ZoneId zone) {
    public OwnershipProperties {
        zone = zone == null ? ZoneId.of("Asia/Dhaka") : zone;
    }
}
