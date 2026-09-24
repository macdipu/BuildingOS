package com.buildingos.building.membership.presentation.rest.request;

public record RevokeMembershipRequest(String reason, Long expectedVersion) {}
