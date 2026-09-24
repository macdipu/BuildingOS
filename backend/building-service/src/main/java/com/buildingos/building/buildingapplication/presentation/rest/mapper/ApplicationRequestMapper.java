package com.buildingos.building.buildingapplication.presentation.rest.mapper;

import com.buildingos.building.buildingapplication.domain.model.ApplicantRelationship;
import com.buildingos.building.buildingapplication.domain.model.ApplicationDetails;
import com.buildingos.building.buildingapplication.domain.model.BuildingType;
import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import com.buildingos.building.buildingapplication.domain.model.Coordinates;
import com.buildingos.building.buildingapplication.domain.model.ManagementType;
import com.buildingos.building.buildingapplication.presentation.rest.request.ApplicationRequest;
import com.buildingos.building.shared.presentation.rest.Enums;

public final class ApplicationRequestMapper {
    private ApplicationRequestMapper() {}

    public static ApplicationDetails toDetails(ApplicationRequest body) {
        if (body == null) {
            return ApplicationDetails.empty();
        }
        return new ApplicationDetails(body.buildingName(),
                Enums.parseOptional(BuildingType.class, body.buildingType(), "buildingType"), body.address(),
                body.area(), body.district(), body.postalCode(), body.totalFloors(), body.estimatedUnits(),
                Enums.parseOptional(ApplicantRelationship.class, body.applicantRelationship(), "applicantRelationship"),
                body.relationshipNote(), body.contactName(),
                body.contactPhone() == null || body.contactPhone().isBlank() ? null
                        : ContactPhone.parse(body.contactPhone()),
                body.contactEmail(), Enums.parseOptional(ManagementType.class, body.managementType(), "managementType"),
                Coordinates.ofNullable(body.latitude(), body.longitude()));
    }
}
