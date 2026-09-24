package com.buildingos.building.buildingapplication.application.updateapplication;

import com.buildingos.building.buildingapplication.domain.model.ApplicationDetails;
import java.util.UUID;

public record UpdateApplicationCommand(UUID applicationId, ApplicationDetails details) {}
