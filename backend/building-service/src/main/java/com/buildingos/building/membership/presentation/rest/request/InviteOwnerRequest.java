package com.buildingos.building.membership.presentation.rest.request;

/** {@code role} must be OWNER; staff/delegation invitations need a separate permission design. */
public record InviteOwnerRequest(String phone, String role, String reason) {}
