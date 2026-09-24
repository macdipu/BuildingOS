package com.buildingos.building.duplicate;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.building.buildingapplication.domain.model.ApplicantRelationship;
import com.buildingos.building.buildingapplication.domain.model.ApplicationDetails;
import com.buildingos.building.buildingapplication.domain.model.BuildingType;
import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import com.buildingos.building.buildingapplication.domain.model.Coordinates;
import com.buildingos.building.duplicate.domain.model.CandidateKind;
import com.buildingos.building.duplicate.domain.model.DuplicateCandidate;
import com.buildingos.building.duplicate.domain.model.DuplicateMatcher;
import com.buildingos.building.duplicate.domain.model.DuplicateSignal;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DuplicateMatcherTest {
    private final DuplicateMatcher matcher = new DuplicateMatcher(Set.of("tower", "road", "rd", "house"), 100);

    private static ApplicationDetails subject() {
        return new ApplicationDetails("Rose Garden Tower", BuildingType.RESIDENTIAL, "House 12, Road 5", "Dhanmondi",
                "Dhaka", null, null, 20, ApplicantRelationship.OWNER, null, "Rahim",
                ContactPhone.parse("01712345678"), null, null, new Coordinates(23.746100, 90.374200));
    }

    private static DuplicateCandidate candidate(String name, String address, String area, String district,
            String phone, Coordinates point) {
        return new DuplicateCandidate(CandidateKind.BUILDING, UUID.randomUUID(), name, "ACTIVE", name, address, area,
                district, phone, point);
    }

    @Test
    void normalizesCasePunctuationSpacingAndFillerWords() {
        assertThat(matcher.normalize("  ROSE-garden   TOWER! ")).isEqualTo("rose garden");
        assertThat(matcher.normalize("House #12, Rd. 5")).isEqualTo("12 5");
        assertThat(matcher.normalize("রোজ গার্ডেন")).isEqualTo("রোজ গার্ডেন");
        assertThat(matcher.normalize(null)).isEmpty();
    }

    @Test
    void reportsEachMatchedSignal() {
        var all = candidate("rose garden", "12 Road 5", "DHANMONDI", "dhaka", "01712345678",
                new Coordinates(23.746500, 90.374200));
        var matches = matcher.match(subject(), List.of(all));
        assertThat(matches).singleElement().satisfies(m -> assertThat(m.signals()).containsExactlyInAnyOrder(
                DuplicateSignal.NAME, DuplicateSignal.ADDRESS, DuplicateSignal.CONTACT_PHONE,
                DuplicateSignal.COORDINATES));
    }

    @Test
    void addressNeedsSameDistrictAndFarPointsDoNotMatch() {
        var otherDistrict = candidate("Lake View", "House 12 Road 5", "Dhanmondi", "Chattogram", null,
                new Coordinates(23.760000, 90.374200));
        assertThat(matcher.match(subject(), List.of(otherDistrict))).isEmpty();
        var unrelated = candidate("Lake View", "House 99", "Gulshan", "Dhaka", "01899999999", null);
        assertThat(matcher.match(subject(), List.of(unrelated))).isEmpty();
    }

    @Test
    void haversineIsAccurateEnough() {
        double meters = DuplicateMatcher.distanceMeters(new Coordinates(23.7461, 90.3742),
                new Coordinates(23.7470, 90.3742));
        assertThat(meters).isBetween(95.0, 105.0);
    }
}
