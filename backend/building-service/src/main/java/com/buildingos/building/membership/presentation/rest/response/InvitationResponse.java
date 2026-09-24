package com.buildingos.building.membership.presentation.rest.response;

import com.buildingos.building.membership.application.InvitationView;
import java.time.Instant;
import java.util.UUID;

/** Admin view; the phone is the one the admin entered. */
public record InvitationResponse(UUID id, UUID buildingId, String phone, String role, String status,
        Instant createdAt, Instant expiresAt, UUID claimedUserId, Instant claimedAt, Instant revokedAt,
        long version) {
    public static InvitationResponse of(InvitationView view) {
        var i = view.invitation();
        return new InvitationResponse(i.id(), i.buildingId(), i.phone().value(), i.role().name(),
                view.status().name(), i.createdAt(), i.expiresAt(), i.claimedUserId(), i.claimedAt(), i.revokedAt(),
                i.version());
    }
}
