package com.buildingos.building.buildingapplication.application.approveapplication;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;

public record ApprovalResult(BuildingApplication application, Building building) {}
