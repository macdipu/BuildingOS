package com.buildingos.building.membership.presentation.rest.response;

import com.buildingos.building.membership.application.listmyinvitations.MyInvitation;
import java.time.Instant;
import java.util.UUID;

/** Recipient view: building identity to decide on, no inviter or other members' data. */
public record MyInvitationResponse(UUID id, UUID buildingId, String buildingName, String buildingAddress,
        String buildingStatus, String role, Instant expiresAt) {
    public static MyInvitationResponse of(MyInvitation mine) {
        var i = mine.invitation();
        var b = mine.building();
        return new MyInvitationResponse(i.id(), b.id(), b.name(), b.address(), b.status().name(), i.role().name(),
                i.expiresAt());
    }
}
