package com.buildingos.building.membership.application.revokemembership;

import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.membership.application.MembershipAudit;
import com.buildingos.building.membership.application.MembershipErrors;
import com.buildingos.building.building.domain.model.BuildingMembership;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.AuditEntry;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import java.time.Clock;

/** Removes an OWNER's building access under the building lock; ownership history is untouched. */
public final class RevokeMembershipService implements RevokeMembershipUseCase {
    private final BuildingAccess access;
    private final BuildingMembershipRepository memberships;
    private final AuditRepository audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public RevokeMembershipService(BuildingAccess access, BuildingMembershipRepository memberships,
            AuditRepository audit, UnitOfWork unitOfWork, Clock clock) {
        this.access = access;
        this.memberships = memberships;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public BuildingMembership execute(Actor actor, RevokeMembershipCommand command) {
        String reason = BuildingApplication.reason(command.reason(), "reason");
        if (command.expectedVersion() == null) {
            throw new IllegalArgumentException("expectedVersion is required");
        }
        return unitOfWork.inTransaction(() -> {
            access.requireAdminToWrite(actor, command.buildingId());
            var current = memberships.findInBuilding(command.buildingId(), command.membershipId())
                    .orElseThrow(MembershipErrors::membershipNotFound);
            var now = clock.instant();
            var revoked = current.revoked(actor.userId(), reason, command.expectedVersion(), now);
            memberships.update(revoked);
            audit.append(AuditEntry.of(revoked.buildingId(), actor.userId(), "MEMBERSHIP_REVOKED",
                    MembershipAudit.MEMBERSHIP, revoked.id(), reason, MembershipAudit.of(current),
                    MembershipAudit.of(revoked), now));
            return revoked;
        });
    }
}
