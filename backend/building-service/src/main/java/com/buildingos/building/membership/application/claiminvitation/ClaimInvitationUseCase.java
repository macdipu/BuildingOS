package com.buildingos.building.membership.application.claiminvitation;

import com.buildingos.building.membership.application.VerifiedPhoneIdentity;
import com.buildingos.building.building.domain.model.BuildingMembership;

public interface ClaimInvitationUseCase {
    BuildingMembership execute(VerifiedPhoneIdentity identity, ClaimInvitationCommand command);
}
