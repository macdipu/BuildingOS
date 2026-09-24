package com.buildingos.building.building.presentation.rest.response;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.model.BuildingMembership;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record BuildingResponse(UUID id, UUID applicationId, String name, String buildingType, String address,
        String area, String district, String postalCode, Double latitude, Double longitude, String contactPhone,
        String status, List<Member> members, Instant createdAt, Instant updatedAt) {
    public record Member(UUID userId, String role, String status) {
        static Member of(BuildingMembership m) { return new Member(m.userId(), m.role().name(), m.status().name()); }
    }

    public static BuildingResponse of(Building b, List<BuildingMembership> memberships) {
        var point = b.coordinates();
        return new BuildingResponse(b.id(), b.applicationId(), b.name(), b.type().name(), b.address(), b.area(),
                b.district(), b.postalCode(), point == null ? null : point.latitude(),
                point == null ? null : point.longitude(), b.contactPhone().value(), b.status().name(),
                memberships.stream().map(Member::of).toList(), b.createdAt(), b.updatedAt());
    }
}
