package com.buildingos.auth.auth.presentation.rest.response;

import java.util.UUID;

public record ProvisionedUserResponse(UUID userId, String phone) {}
