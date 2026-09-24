package com.buildingos.building.building;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.model.BuildingStatus;
import com.buildingos.building.building.domain.model.NoBuildingAdminException;
import com.buildingos.building.buildingapplication.domain.model.ApplicantRelationship;
import com.buildingos.building.buildingapplication.domain.model.ApplicationDetails;
import com.buildingos.building.buildingapplication.domain.model.ApplicationNumber;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.buildingapplication.domain.model.BuildingType;
import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import com.buildingos.building.buildingapplication.domain.model.InvalidTransitionException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuildingTest {
    private static final Instant NOW = Instant.parse("2026-09-24T06:00:00Z");

    private static Building onboarding() {
        var details = new ApplicationDetails("Rose Garden", BuildingType.MIXED, "House 12", "Dhanmondi", "Dhaka", null,
                null, 12, ApplicantRelationship.OWNER, null, "Rahim", ContactPhone.parse("01712345678"), null, null,
                null);
        var reviewer = UUID.randomUUID();
        var approved = BuildingApplication.draft(UUID.randomUUID(), ApplicationNumber.of(2026, 7), UUID.randomUUID(),
                details, NOW).submitted(NOW).reviewStarted(reviewer, NOW).approved(reviewer, NOW);
        return Building.fromApproved(approved, NOW);
    }

    @Test
    void copiesIdentityFromTheApprovedApplication() {
        var building = onboarding();
        assertThat(building.status()).isEqualTo(BuildingStatus.ONBOARDING);
        assertThat(building.name()).isEqualTo("Rose Garden");
        assertThat(building.contactPhone().value()).isEqualTo("01712345678");
    }

    @Test
    void activationNeedsAnAdminThenSuspendAndReactivate() {
        var building = onboarding();
        assertThatThrownBy(() -> building.activated(0, NOW)).isInstanceOf(NoBuildingAdminException.class);
        var active = building.activated(1, NOW);
        assertThat(active.status()).isEqualTo(BuildingStatus.ACTIVE);
        assertThatThrownBy(() -> active.activated(1, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> active.reactivated(NOW)).isInstanceOf(InvalidTransitionException.class);
        var suspended = active.suspended(NOW);
        assertThat(suspended.status()).isEqualTo(BuildingStatus.SUSPENDED);
        assertThatThrownBy(() -> building.suspended(NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThat(suspended.reactivated(NOW).status()).isEqualTo(BuildingStatus.ACTIVE);
        assertThat(suspended.reactivated(NOW).version()).isEqualTo(3);
    }
}
