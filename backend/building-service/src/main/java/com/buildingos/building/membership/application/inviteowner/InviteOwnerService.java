package com.buildingos.building.membership.application.inviteowner;

import com.buildingos.building.building.domain.model.BuildingRole;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.membership.application.InvitationView;
import com.buildingos.building.membership.application.MembershipAudit;
import com.buildingos.building.membership.domain.model.BuildingInvitation;
import com.buildingos.building.membership.domain.repository.BuildingInvitationRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.Map;

/** Admin invites an OWNER by phone; one live invitation per building/phone/role (UO-01/05). */
public final class InviteOwnerService implements InviteOwnerUseCase {
    private final BuildingAccess access;
    private final BuildingInvitationRepository invitations;
    private final AuditRepository audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;
    private final Duration ttl;

    public InviteOwnerService(BuildingAccess access, BuildingInvitationRepository invitations, AuditRepository audit,
            UnitOfWork unitOfWork, Clock clock, Duration ttl) {
        this.access = access;
        this.invitations = invitations;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
        this.ttl = ttl;
    }

    @Override
    public InvitationResult execute(Actor actor, InviteOwnerCommand command) {
        var phone = ContactPhone.parse(command.phone());
        String reason = BuildingApplication.reason(command.reason(), "reason");
        return unitOfWork.inTransaction(() -> {
            access.requireAdminToWrite(actor, command.buildingId());
            var now = clock.instant();
            var pending = invitations.findPending(command.buildingId(), phone, BuildingRole.OWNER);
            if (pending.isPresent()) {
                if (pending.get().isLive(now)) {
                    return new InvitationResult(InvitationView.at(pending.get(), now), false);
                }
                invitations.update(pending.get().expired());
            }
            var invitation = BuildingInvitation.owner(command.buildingId(), phone, actor.userId(), reason, now, ttl);
            invitations.insert(invitation);
            audit.append(AuditEntry.of(invitation.buildingId(), actor.userId(), "INVITATION_CREATED",
                    MembershipAudit.INVITATION, invitation.id(), reason, Map.of(), MembershipAudit.of(invitation), now));
            return new InvitationResult(InvitationView.at(invitation, now), true);
        });
    }
}
