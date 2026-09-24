package com.buildingos.building.buildingapplication.application.rejectapplication;

import java.util.UUID;

public record RejectApplicationCommand(UUID applicationId, String reason) {}
