package com.buildingos.auth.auth.application.provisionuser;

import java.util.UUID;

public record ProvisionUserResult(UUID userId, String phone) {}
