package com.buildingos.account.auth.presentation.rest.response;

import java.util.List;
import java.util.UUID;

public record VerifiedUserResponse(UUID id, String phone, List<String> platformRoles) {}
