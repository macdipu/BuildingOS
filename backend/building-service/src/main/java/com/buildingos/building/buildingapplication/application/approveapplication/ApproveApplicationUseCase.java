package com.buildingos.building.buildingapplication.application.approveapplication;

import com.buildingos.building.shared.application.Actor;

public interface ApproveApplicationUseCase {
    ApprovalResult execute(Actor actor, ApproveApplicationCommand command);
}
