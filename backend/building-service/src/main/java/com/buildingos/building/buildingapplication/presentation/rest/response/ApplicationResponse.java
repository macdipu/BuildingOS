package com.buildingos.building.buildingapplication.presentation.rest.response;

import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** {@code reviewedBy} is shown to platform admins only. */
public record ApplicationResponse(UUID id, String applicationNumber, UUID applicantUserId, String status,
        String source, String buildingName, String buildingType, String address, String area, String district,
        String postalCode, Integer totalFloors, Integer estimatedUnits, String applicantRelationship,
        String relationshipNote, String contactName, String contactPhone, String contactEmail, String managementType,
        Double latitude, Double longitude, List<String> missingFields, Instant submittedAt, Instant reviewedAt,
        UUID reviewedBy, String rejectionReason, String infoRequestMessage, Instant createdAt, Instant updatedAt) {
    public static ApplicationResponse of(BuildingApplication a, boolean forPlatformAdmin) {
        var d = a.details();
        var point = d.coordinates();
        return new ApplicationResponse(a.id(), a.number().value(), a.applicantUserId(), a.status().name(),
                a.source().name(), d.buildingName(), name(d.buildingType()), d.address(), d.area(), d.district(),
                d.postalCode(), d.totalFloors(), d.estimatedUnits(), name(d.applicantRelationship()),
                d.relationshipNote(), d.contactName(), d.contactPhone() == null ? null : d.contactPhone().value(),
                d.contactEmail(), name(d.managementType()), point == null ? null : point.latitude(),
                point == null ? null : point.longitude(), d.missingForSubmission(), a.submittedAt(), a.reviewedAt(),
                forPlatformAdmin ? a.reviewedBy() : null, a.rejectionReason(), a.infoRequestMessage(), a.createdAt(),
                a.updatedAt());
    }

    private static String name(Enum<?> value) { return value == null ? null : value.name(); }
}
