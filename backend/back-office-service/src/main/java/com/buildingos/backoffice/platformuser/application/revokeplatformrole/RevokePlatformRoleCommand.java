package com.buildingos.backoffice.platformuser.application.revokeplatformrole;

import java.util.UUID;

public record RevokePlatformRoleCommand(UUID userId, String role) {}
