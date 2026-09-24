package com.buildingos.building.membership.application.listmyinvitations;

import com.buildingos.building.membership.application.VerifiedPhoneIdentity;
import java.util.List;

public interface ListMyInvitationsUseCase {
    List<MyInvitation> execute(VerifiedPhoneIdentity identity);
}
