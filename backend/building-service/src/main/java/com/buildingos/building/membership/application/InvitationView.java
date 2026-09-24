package com.buildingos.building.membership.application;

import com.buildingos.building.membership.domain.model.BuildingInvitation;
import com.buildingos.building.membership.domain.model.InvitationStatus;
import java.time.Instant;

/** {@code status} is the effective status: a PENDING invitation past its expiry reads as EXPIRED. */
public record InvitationView(BuildingInvitation invitation, InvitationStatus status) {
    public static InvitationView at(BuildingInvitation invitation, Instant now) {
        return new InvitationView(invitation, invitation.effectiveStatus(now));
    }
}
