package com.buildingos.building.buildingapplication.application.approveapplication;

import com.buildingos.building.building.domain.model.Building;
import com.buildingos.building.building.domain.model.BuildingMembership;
import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.building.domain.repository.BuildingRepository;
import com.buildingos.building.buildingapplication.application.ApplicationErrors;
import com.buildingos.building.buildingapplication.application.port.out.CreationFeeGateway;
import com.buildingos.building.buildingapplication.application.port.out.UserProvisioning;
import com.buildingos.building.buildingapplication.domain.model.BuildingApplication;
import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import com.buildingos.building.buildingapplication.domain.repository.BuildingApplicationRepository;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.BusinessRuleException;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.EntityType;
import com.buildingos.building.shared.domain.model.LifecycleTransition;
import com.buildingos.building.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Clock;

/**
 * UNDER_REVIEW to APPROVED (BRD 149.7 as amended by D-26): checks the creation fee, provisions the chosen admin
 * (D-29), then in one transaction approves, creates the building in ONBOARDING and assigns BUILDING_ADMIN. Remote
 * calls happen before the transaction; a failure there changes nothing. No subscription is attached.
 */
public final class ApproveApplicationService implements ApproveApplicationUseCase {
    private final BuildingApplicationRepository applications;
    private final BuildingRepository buildings;
    private final BuildingMembershipRepository memberships;
    private final LifecycleTransitionRepository transitions;
    private final CreationFeeGateway fees;
    private final UserProvisioning users;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public ApproveApplicationService(BuildingApplicationRepository applications, BuildingRepository buildings,
            BuildingMembershipRepository memberships, LifecycleTransitionRepository transitions,
            CreationFeeGateway fees, UserProvisioning users, UnitOfWork unitOfWork, Clock clock) {
        this.applications = applications;
        this.buildings = buildings;
        this.memberships = memberships;
        this.transitions = transitions;
        this.fees = fees;
        this.users = users;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public ApprovalResult execute(Actor actor, ApproveApplicationCommand command) {
        actor.requirePlatformAdmin();
        var adminPhone = ContactPhone.parse(command.adminPhone());
        var reason = BuildingApplication.reason(command.reason(), "reason");
        var preview = applications.findById(command.applicationId())
                .orElseThrow(() -> ApplicationErrors.notFound(command.applicationId()));
        preview.approved(actor.userId(), clock.instant());

        if (!fees.status(preview.id()).allowsApproval()) {
            throw BusinessRuleException.conflict("CREATION_FEE_UNPAID",
                    "The building-creation fee must be recorded before approval");
        }
        var adminUserId = users.provision(adminPhone);

        return unitOfWork.inTransaction(() -> {
            var current = applications.findByIdForUpdate(preview.id())
                    .orElseThrow(() -> ApplicationErrors.notFound(preview.id()));
            var now = clock.instant();
            var approved = current.approved(actor.userId(), now);
            applications.update(approved);
            var building = Building.fromApproved(approved, now);
            buildings.insert(building);
            memberships.insert(BuildingMembership.admin(building.id(), adminUserId, now));
            transitions.append(LifecycleTransition.of(EntityType.BUILDING_APPLICATION, approved.id(), current.status(),
                    approved.status(), actor.userId(), reason, now));
            transitions.append(LifecycleTransition.of(EntityType.BUILDING, building.id(), null, building.status(),
                    actor.userId(), reason, now));
            return new ApprovalResult(approved, building);
        });
    }
}
