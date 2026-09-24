package com.buildingos.building.membership.application.claiminvitation;

import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.membership.application.MembershipAudit;
import com.buildingos.building.membership.application.MembershipErrors;
import com.buildingos.building.membership.application.VerifiedPhoneIdentity;
import com.buildingos.building.building.domain.model.BuildingMembership;
import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.building.domain.repository.BuildingRepository;
import com.buildingos.building.membership.domain.model.BuildingInvitation;
import com.buildingos.building.membership.domain.model.InvitationStatus;
import com.buildingos.building.membership.domain.repository.BuildingInvitationRepository;
import com.buildingos.building.shared.application.Fingerprints;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.model.RecordedOperation;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import com.buildingos.building.shared.domain.repository.OperationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Binds an invitation to the verified subject and grants OWNER membership, never ownership (UO-05, ADR-F4-001).
 * Only the invited phone can see or claim it; a repeated claim by the same subject returns its membership.
 */
public final class ClaimInvitationService implements ClaimInvitationUseCase {
    static final String ACTION = "CLAIM_INVITATION";
    private static final String CLAIM_REASON = "Claimed by the invited phone owner";

    private final BuildingInvitationRepository invitations;
    private final BuildingRepository buildings;
    private final BuildingMembershipRepository memberships;
    private final OperationRepository operations;
    private final AuditRepository audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public ClaimInvitationService(BuildingInvitationRepository invitations, BuildingRepository buildings,
            BuildingMembershipRepository memberships, OperationRepository operations, AuditRepository audit,
            UnitOfWork unitOfWork, Clock clock) {
        this.invitations = invitations;
        this.buildings = buildings;
        this.memberships = memberships;
        this.operations = operations;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public BuildingMembership execute(VerifiedPhoneIdentity identity, ClaimInvitationCommand command) {
        if (command.operationId() == null) {
            throw new IllegalArgumentException("operationId is required");
        }
        UUID user = identity.userId();
        String fingerprint = Fingerprints.of(ACTION, command.invitationId());
        return unitOfWork.inTransaction(() -> {
            var located = addressedTo(identity, command.invitationId());
            var building = buildings.findByIdForUpdate(located.buildingId()).orElseThrow();
            var replay = operations.find(user, ACTION, command.operationId());
            if (replay.isPresent()) {
                if (!replay.get().requestFingerprint().equals(fingerprint)) {
                    throw MembershipErrors.idempotencyConflict();
                }
                return activeMembership(building.id(), user).orElseThrow(MembershipErrors::invitationNotFound);
            }
            var invitation = addressedTo(identity, command.invitationId());
            if (invitation.status() == InvitationStatus.CLAIMED && user.equals(invitation.claimedUserId())) {
                var existing = activeMembership(building.id(), user);
                if (existing.isPresent()) {
                    return existing.get();
                }
            }
            BuildingAccess.requireWritable(building);
            var now = clock.instant();
            var claimed = invitation.claimed(user, now);
            var membership = grant(building.id(), user, now);
            invitations.update(claimed);
            audit.append(AuditEntry.of(building.id(), user, "INVITATION_CLAIMED", MembershipAudit.INVITATION,
                    claimed.id(), CLAIM_REASON, MembershipAudit.of(invitation), MembershipAudit.of(claimed), now));
            operations.insert(new RecordedOperation(user, ACTION, command.operationId(), building.id(), fingerprint,
                    membership.id(), membership.version(), now));
            return membership;
        });
    }

    private BuildingInvitation addressedTo(VerifiedPhoneIdentity identity, UUID invitationId) {
        return invitations.findById(invitationId).filter(i -> i.phone().equals(identity.phone()))
                .orElseThrow(MembershipErrors::invitationNotFound);
    }

    private Optional<BuildingMembership> activeMembership(UUID buildingId, UUID user) {
        return memberships.find(buildingId, user, BuildingRole.OWNER).filter(BuildingMembership::isActive);
    }

    private BuildingMembership grant(UUID buildingId, UUID user, Instant now) {
        var existing = memberships.find(buildingId, user, BuildingRole.OWNER);
        if (existing.isPresent() && existing.get().isActive()) {
            return existing.get();
        }
        BuildingMembership granted;
        String action;
        if (existing.isPresent()) {
            granted = existing.get().reinstated(now);
            memberships.update(granted);
            action = "MEMBERSHIP_REINSTATED";
        } else {
            granted = BuildingMembership.owner(buildingId, user, now);
            memberships.insert(granted);
            action = "MEMBERSHIP_GRANTED";
        }
        audit.append(AuditEntry.of(buildingId, user, action, MembershipAudit.MEMBERSHIP, granted.id(), CLAIM_REASON,
                existing.map(MembershipAudit::of).orElse(Map.of()), MembershipAudit.of(granted), now));
        return granted;
    }
}
