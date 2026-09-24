package com.buildingos.building.membership.application.revokeinvitation;

import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.membership.application.InvitationView;
import com.buildingos.building.membership.application.MembershipAudit;
import com.buildingos.building.membership.application.MembershipErrors;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.membership.domain.repository.BuildingInvitationRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import java.time.Clock;

/** Revokes a live invitation; the row and its audit remain. */
public final class RevokeInvitationService implements RevokeInvitationUseCase {
    private final BuildingAccess access;
    private final BuildingInvitationRepository invitations;
    private final AuditRepository audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public RevokeInvitationService(BuildingAccess access, BuildingInvitationRepository invitations,
            AuditRepository audit, UnitOfWork unitOfWork, Clock clock) {
        this.access = access;
        this.invitations = invitations;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public InvitationView execute(Actor actor, RevokeInvitationCommand command) {
        String reason = BuildingApplication.reason(command.reason(), "reason");
        return unitOfWork.inTransaction(() -> {
            access.requireAdminToWrite(actor, command.buildingId());
            var current = invitations.findInBuilding(command.buildingId(), command.invitationId())
                    .orElseThrow(MembershipErrors::invitationNotFound);
            var now = clock.instant();
            var revoked = current.revoked(actor.userId(), reason, now);
            invitations.update(revoked);
            audit.append(AuditEntry.of(revoked.buildingId(), actor.userId(), "INVITATION_REVOKED",
                    MembershipAudit.INVITATION, revoked.id(), reason, MembershipAudit.of(current),
                    MembershipAudit.of(revoked), now));
            return InvitationView.at(revoked, now);
        });
    }
}
