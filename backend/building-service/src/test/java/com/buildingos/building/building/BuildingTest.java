package com.buildingos.building.building;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.building.building.domain.model.ActivationReadiness;
import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.model.BuildingStatus;
import com.buildingos.building.building.domain.model.NoBuildingAdminException;
import com.buildingos.building.building.domain.model.NoBuildingUnitException;
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
    void activationNeedsAnAdminAndAUnitThenSuspendAndReactivate() {
        var building = onboarding();
        var ready = new ActivationReadiness(1, 1);
        assertThatThrownBy(() -> building.activated(new ActivationReadiness(0, 1), NOW))
                .isInstanceOf(NoBuildingAdminException.class);
        assertThatThrownBy(() -> building.activated(new ActivationReadiness(1, 0), NOW))
                .isInstanceOf(NoBuildingUnitException.class);
        var active = building.activated(ready, NOW);
        assertThat(active.status()).isEqualTo(BuildingStatus.ACTIVE);
        assertThatThrownBy(() -> active.activated(ready, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> active.reactivated(ready, NOW)).isInstanceOf(InvalidTransitionException.class);
        var suspended = active.suspended(NOW);
        assertThat(suspended.status()).isEqualTo(BuildingStatus.SUSPENDED);
        assertThatThrownBy(() -> building.suspended(NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> suspended.reactivated(new ActivationReadiness(1, 0), NOW))
                .isInstanceOf(NoBuildingUnitException.class);
        assertThat(suspended.reactivated(ready, NOW).status()).isEqualTo(BuildingStatus.ACTIVE);
        assertThat(suspended.reactivated(ready, NOW).version()).isEqualTo(3);
    }
}
