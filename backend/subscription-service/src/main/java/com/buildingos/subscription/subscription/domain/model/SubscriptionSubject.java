package com.buildingos.subscription.subscription.domain.model;

import java.util.Objects;
import java.util.UUID;

public record SubscriptionSubject(SubjectType type, UUID id) {
    public SubscriptionSubject {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
    }

    public static SubscriptionSubject user(UUID userId) {
        return new SubscriptionSubject(SubjectType.USER, userId);
    }
}
