package com.buildingos.subscription.fee.domain.model;

import java.util.Objects;
import java.util.UUID;

/** What a payment is for, e.g. a building application. */
public record PaymentReference(ReferenceType type, UUID id) {
    public PaymentReference {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(id, "id");
    }
}
