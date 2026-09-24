package com.buildingos.building.membership.application.listmyinvitations;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.membership.domain.model.BuildingInvitation;

public record MyInvitation(BuildingInvitation invitation, Building building) {}
