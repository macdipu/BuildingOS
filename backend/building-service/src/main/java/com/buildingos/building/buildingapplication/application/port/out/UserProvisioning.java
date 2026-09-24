package com.buildingos.building.buildingapplication.application.port.out;

import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import java.util.UUID;

/** Finds or creates the global user for a phone in auth-service (D-29); idempotent. */
public interface UserProvisioning {
    UUID provision(ContactPhone phone);
}
