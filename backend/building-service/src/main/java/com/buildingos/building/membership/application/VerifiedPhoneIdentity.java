package com.buildingos.building.membership.application;

import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import java.util.Objects;
import java.util.UUID;

/** Subject and canonical phone from a validated auth-service token; never from a request body. */
public record VerifiedPhoneIdentity(UUID userId, ContactPhone phone) {
    public VerifiedPhoneIdentity {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(phone, "phone");
    }
}
