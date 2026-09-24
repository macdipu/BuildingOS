package com.buildingos.building.buildingapplication.presentation.rest.request;

/** Every field optional while drafting; submission checks completeness. */
public record ApplicationRequest(String buildingName, String buildingType, String address, String area,
        String district, String postalCode, Integer totalFloors, Integer estimatedUnits, String applicantRelationship,
        String relationshipNote, String contactName, String contactPhone, String contactEmail, String managementType,
        Double latitude, Double longitude) {}
