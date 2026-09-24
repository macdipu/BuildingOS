package com.buildingos.building.membership.infrastructure.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** {@code buildingos.membership.*}; invitation TTL defaults to 7 days (TECH-SPEC-F4). */
@ConfigurationProperties("buildingos.membership")
public record MembershipProperties(Duration invitationTtl) {
    public MembershipProperties {
        if (invitationTtl == null) {
            invitationTtl = Duration.ofDays(7);
        }
        if (invitationTtl.isNegative() || invitationTtl.isZero()) {
            throw new IllegalArgumentException("buildingos.membership.invitation-ttl must be positive");
        }
    }
}
