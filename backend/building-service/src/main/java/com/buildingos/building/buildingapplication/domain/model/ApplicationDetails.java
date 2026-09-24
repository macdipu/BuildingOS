package com.buildingos.building.buildingapplication.domain.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Applicant-editable fields (AP-01). Any field may be empty while drafting; {@link #missingForSubmission()}
 * lists what submission still needs. Formats and ranges are checked on every edit.
 */
public record ApplicationDetails(String buildingName, BuildingType buildingType, String address, String area,
        String district, String postalCode, Integer totalFloors, Integer estimatedUnits,
        ApplicantRelationship applicantRelationship, String relationshipNote, String contactName,
        ContactPhone contactPhone, String contactEmail, ManagementType managementType, Coordinates coordinates) {
    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    public ApplicationDetails {
        buildingName = text(buildingName, "buildingName", 200);
        address = text(address, "address", 500);
        area = text(area, "area", 120);
        district = text(district, "district", 120);
        postalCode = text(postalCode, "postalCode", 16);
        relationshipNote = applicantRelationship == ApplicantRelationship.OTHER
                ? text(relationshipNote, "relationshipNote", 200) : null;
        contactName = text(contactName, "contactName", 200);
        contactEmail = text(contactEmail, "contactEmail", 254);
        positive(totalFloors, "totalFloors");
        positive(estimatedUnits, "estimatedUnits");
        if (contactEmail != null && !EMAIL.matcher(contactEmail).matches()) {
            throw new IllegalArgumentException("contactEmail is not a valid email address");
        }
    }

    public static ApplicationDetails empty() {
        return new ApplicationDetails(null, null, null, null, null, null, null, null, null, null, null, null, null,
                null, null);
    }

    public List<String> missingForSubmission() {
        var missing = new ArrayList<String>();
        require(missing, buildingName, "buildingName");
        require(missing, buildingType, "buildingType");
        require(missing, address, "address");
        require(missing, area, "area");
        require(missing, district, "district");
        require(missing, estimatedUnits, "estimatedUnits");
        require(missing, applicantRelationship, "applicantRelationship");
        if (applicantRelationship == ApplicantRelationship.OTHER) {
            require(missing, relationshipNote, "relationshipNote");
        }
        require(missing, contactName, "contactName");
        require(missing, contactPhone, "contactPhone");
        return List.copyOf(missing);
    }

    private static void require(List<String> missing, Object value, String field) {
        if (value == null) {
            missing.add(field);
        }
    }

    private static String text(String value, String field, int max) {
        if (value == null) {
            return null;
        }
        String trimmed = value.strip();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > max) {
            throw new IllegalArgumentException(field + " must be at most " + max + " characters");
        }
        return trimmed;
    }

    private static void positive(Integer value, String field) {
        if (value != null && value <= 0) {
            throw new IllegalArgumentException(field + " must be positive");
        }
    }
}
