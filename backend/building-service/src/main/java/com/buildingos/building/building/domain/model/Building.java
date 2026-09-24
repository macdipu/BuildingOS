package com.buildingos.building.building.domain.model;

import com.buildingos.building.buildingapplication.domain.model.ApplicationDetails;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.buildingapplication.domain.model.BuildingType;
import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import com.buildingos.building.buildingapplication.domain.model.Coordinates;
import com.buildingos.building.buildingapplication.domain.model.InvalidTransitionException;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/** Canonical building created on approval (BRD 149.7); identity data copied from the approved application. */
public record Building(UUID id, UUID applicationId, String name, BuildingType type, String address, String area,
        String district, String postalCode, Coordinates coordinates, ContactPhone contactPhone, BuildingStatus status,
        int version, Instant createdAt, Instant updatedAt) {
    public Building {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(applicationId, "applicationId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(address, "address");
        Objects.requireNonNull(area, "area");
        Objects.requireNonNull(district, "district");
        Objects.requireNonNull(contactPhone, "contactPhone");
        Objects.requireNonNull(status, "status");
    }

    public static Building fromApproved(BuildingApplication application, Instant at) {
        ApplicationDetails d = application.details();
        return new Building(UUID.randomUUID(), application.id(), d.buildingName(), d.buildingType(), d.address(),
                d.area(), d.district(), d.postalCode(), d.coordinates(), d.contactPhone(), BuildingStatus.ONBOARDING,
                0, at, at);
    }

    /** Interim rule (D-30): an active building admin is the only prerequisite until the units slice lands. */
    public Building activated(long activeAdmins, Instant at) {
        require(BuildingStatus.ONBOARDING, "activate");
        if (activeAdmins < 1) {
            throw new NoBuildingAdminException();
        }
        return with(BuildingStatus.ACTIVE, at);
    }

    public Building suspended(Instant at) {
        require(BuildingStatus.ACTIVE, "suspend");
        return with(BuildingStatus.SUSPENDED, at);
    }

    public Building reactivated(Instant at) {
        require(BuildingStatus.SUSPENDED, "reactivate");
        return with(BuildingStatus.ACTIVE, at);
    }

    private void require(BuildingStatus expected, String action) {
        if (status != expected) {
            throw new InvalidTransitionException(status, action);
        }
    }

    private Building with(BuildingStatus newStatus, Instant at) {
        return new Building(id, applicationId, name, type, address, area, district, postalCode, coordinates,
                contactPhone, newStatus, version + 1, createdAt, at);
    }
}
