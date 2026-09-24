package com.buildingos.building.buildingapplication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.building.buildingapplication.domain.model.ApplicantRelationship;
import com.buildingos.building.buildingapplication.domain.model.ApplicationDetails;
import com.buildingos.building.buildingapplication.domain.model.ApplicationNotEditableException;
import com.buildingos.building.buildingapplication.domain.model.ApplicationNumber;
import com.buildingos.building.buildingapplication.domain.model.ApplicationStatus;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.buildingapplication.domain.model.BuildingType;
import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import com.buildingos.building.buildingapplication.domain.model.Coordinates;
import com.buildingos.building.buildingapplication.domain.model.IncompleteApplicationException;
import com.buildingos.building.buildingapplication.domain.model.InvalidTransitionException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class BuildingApplicationTest {
    private static final Instant NOW = Instant.parse("2026-09-24T06:00:00Z");
    private final UUID applicant = UUID.randomUUID();
    private final UUID reviewer = UUID.randomUUID();

    static ApplicationDetails complete(ApplicantRelationship relationship, String note) {
        return new ApplicationDetails("Rose Garden", BuildingType.RESIDENTIAL, "House 12, Road 5", "Dhanmondi",
                "Dhaka", "1205", 10, 36, relationship, note, "Rahim Uddin", ContactPhone.parse("+8801712345678"),
                "rahim@example.com", null, new Coordinates(23.7461, 90.3742));
    }

    private BuildingApplication draft(ApplicationDetails details) {
        return BuildingApplication.draft(UUID.randomUUID(), ApplicationNumber.of(2026, 1), applicant, details, NOW);
    }

    private BuildingApplication underReview() {
        return draft(complete(ApplicantRelationship.OWNER, null)).submitted(NOW).reviewStarted(reviewer, NOW);
    }

    @Test
    void draftSubmitsOnlyWhenComplete() {
        var empty = draft(ApplicationDetails.empty());
        assertThatThrownBy(() -> empty.submitted(NOW)).isInstanceOf(IncompleteApplicationException.class)
                .satisfies(e -> assertThat(((IncompleteApplicationException) e).missingFields()).containsExactly(
                        "buildingName", "buildingType", "address", "area", "district", "estimatedUnits",
                        "applicantRelationship", "contactName", "contactPhone"));

        var submitted = draft(complete(ApplicantRelationship.OWNER, null)).submitted(NOW);
        assertThat(submitted.status()).isEqualTo(ApplicationStatus.SUBMITTED);
        assertThat(submitted.submittedAt()).isEqualTo(NOW);
        assertThat(submitted.version()).isEqualTo(1);
    }

    @Test
    void otherRelationshipNeedsNoteAndNoteIsDroppedOtherwise() {
        var other = draft(complete(ApplicantRelationship.OTHER, null));
        assertThatThrownBy(() -> other.submitted(NOW)).isInstanceOf(IncompleteApplicationException.class)
                .hasMessageContaining("relationshipNote");
        assertThat(complete(ApplicantRelationship.OWNER, "ignored").relationshipNote()).isNull();
        assertThat(complete(ApplicantRelationship.OTHER, "  Tenant rep ").relationshipNote()).isEqualTo("Tenant rep");
    }

    @Test
    void detailsValidateFormats() {
        assertThatThrownBy(() -> new ApplicationDetails(null, null, null, null, null, null, 0, null, null, null, null,
                null, null, null, null)).hasMessageContaining("totalFloors");
        assertThatThrownBy(() -> new ApplicationDetails(null, null, null, null, null, null, null, null, null, null,
                null, null, "not-an-email", null, null)).hasMessageContaining("contactEmail");
        assertThatThrownBy(() -> ContactPhone.parse("01212345678")).isInstanceOf(IllegalArgumentException.class);
        assertThat(ContactPhone.parse("8801912345678").value()).isEqualTo("01912345678");
        assertThatThrownBy(() -> Coordinates.ofNullable(23.0, null)).hasMessageContaining("together");
        assertThatThrownBy(() -> new Coordinates(91, 0)).isInstanceOf(IllegalArgumentException.class);
        assertThat(new ApplicationDetails("  ", null, null, null, null, null, null, null, null, null, null, null,
                null, null, null).buildingName()).isNull();
    }

    @Test
    void reviewPathsFollowTheStateMachine() {
        var review = underReview();
        assertThat(review.status()).isEqualTo(ApplicationStatus.UNDER_REVIEW);
        assertThat(review.reviewedBy()).isEqualTo(reviewer);

        var info = review.informationRequested(reviewer, " Upload the deed ", NOW);
        assertThat(info.status()).isEqualTo(ApplicationStatus.MORE_INFORMATION_REQUIRED);
        assertThat(info.infoRequestMessage()).isEqualTo("Upload the deed");
        assertThat(info.isEditable()).isTrue();
        assertThat(info.submitted(NOW).status()).isEqualTo(ApplicationStatus.SUBMITTED);

        var rejected = review.rejected(reviewer, "Duplicate of BA-2026-000002", NOW);
        assertThat(rejected.status()).isEqualTo(ApplicationStatus.REJECTED);
        assertThat(rejected.reviewedAt()).isEqualTo(NOW);

        assertThat(review.approved(reviewer, NOW).status()).isEqualTo(ApplicationStatus.APPROVED);
    }

    @Test
    void illegalTransitionsAreRejected() {
        var draft = draft(complete(ApplicantRelationship.OWNER, null));
        assertThatThrownBy(() -> draft.reviewStarted(reviewer, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> draft.approved(reviewer, NOW)).isInstanceOf(InvalidTransitionException.class);
        var submitted = draft.submitted(NOW);
        assertThatThrownBy(() -> submitted.submitted(NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> submitted.rejected(reviewer, "x", NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> submitted.edited(ApplicationDetails.empty(), NOW))
                .isInstanceOf(ApplicationNotEditableException.class);
        var rejected = underReview().rejected(reviewer, "no", NOW);
        assertThatThrownBy(() -> rejected.reviewStarted(reviewer, NOW)).isInstanceOf(InvalidTransitionException.class);
        assertThatThrownBy(() -> rejected.approved(reviewer, NOW)).isInstanceOf(InvalidTransitionException.class);
    }

    @Test
    void sensitiveActionsNeedReason() {
        var review = underReview();
        assertThatThrownBy(() -> review.rejected(reviewer, " ", NOW)).hasMessageContaining("reason is required");
        assertThatThrownBy(() -> review.informationRequested(reviewer, null, NOW))
                .hasMessageContaining("message is required");
        assertThatThrownBy(() -> review.rejected(reviewer, "x".repeat(1001), NOW)).hasMessageContaining("at most");
    }

    @Test
    void applicationNumberFormat() {
        assertThat(ApplicationNumber.of(2026, 42).value()).isEqualTo("BA-2026-000042");
        assertThatThrownBy(() -> new ApplicationNumber("X-1")).isInstanceOf(IllegalArgumentException.class);
    }
}
