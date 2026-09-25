package com.buildingos.auth.auth.presentation.rest.request;

import jakarta.validation.constraints.NotBlank;

public record AssignPlatformRoleRequest(@NotBlank String role) {}
