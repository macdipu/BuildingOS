package com.buildingos.subscription.subscription.presentation.rest.response;

import com.buildingos.subscription.subscription.domain.model.PlatformSubscriptionRow;
import java.time.Instant;
import java.util.UUID;

public record PlatformSubscriptionResponse(UUID id, UUID userId, UUID planId, String planCode, String planName,
        String status, String billingCycle, String grantedBy, Instant startedAt) {
    public static PlatformSubscriptionResponse of(PlatformSubscriptionRow row) {
        var s = row.subscription();
        return new PlatformSubscriptionResponse(s.id(), s.subject().id(), s.planId(), row.planCode(), row.planName(),
                s.status().name(), s.billingCycle().name(), s.grantedBy().name(), s.startedAt());
    }
}
