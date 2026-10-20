package com.buildingos.backoffice.platformuser.application.assignplatformrole;

import java.util.UUID;

public record AssignPlatformRoleCommand(UUID userId, String role) {}
