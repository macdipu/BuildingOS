package com.buildingos.building.buildingapplication.application.requestinformation;

import java.util.UUID;

public record RequestInformationCommand(UUID applicationId, String message) {}
