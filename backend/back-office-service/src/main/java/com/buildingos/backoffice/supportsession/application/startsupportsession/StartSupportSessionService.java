package com.buildingos.backoffice.supportsession.application.startsupportsession;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.port.out.BuildingDirectory;
import com.buildingos.backoffice.shared.application.port.out.PlatformUserDirectory;
import com.buildingos.backoffice.shared.application.port.out.UnitOfWork;
import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import com.buildingos.backoffice.supportsession.application.SupportAccess;
import com.buildingos.backoffice.supportsession.application.SupportErrors;
import com.buildingos.backoffice.supportsession.application.SupportSessionDetails;
import com.buildingos.backoffice.supportsession.application.SupportSessionExpiry;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;
import com.buildingos.backoffice.supportsession.domain.model.SupportSession;
import com.buildingos.backoffice.supportsession.domain.repository.ElevatedApprovalRequestRepository;
import com.buildingos.backoffice.supportsession.domain.repository.SupportSessionRepository;
import java.time.Duration;
import java.util.Objects;

/**
 * D-36a/b/d: a support user opens a session for themselves. The target user (auth-service) and building
 * (building-service) are checked before anything is stored, so a dependency failure (503) leaves no row behind.
 * Ordinary scopes are granted; each high-risk scope becomes a PENDING elevated approval request.
 */
public final class StartSupportSessionService implements StartSupportSessionUseCase {
    private final SupportSessionRepository sessions;
    private final ElevatedApprovalRequestRepository approvals;
    private final LifecycleTransitionRepository transitions;
    private final PlatformUserDirectory users;
    private final BuildingDirectory buildings;
    private final SupportSessionExpiry expiry;
    private final UnitOfWork unitOfWork;
    private final Duration maxDuration;

    public StartSupportSessionService(SupportSessionRepository sessions, ElevatedApprovalRequestRepository approvals,
            LifecycleTransitionRepository transitions, PlatformUserDirectory users, BuildingDirectory buildings,
            SupportSessionExpiry expiry, UnitOfWork unitOfWork, Duration maxDuration) {
        this.sessions = sessions;
        this.approvals = approvals;
        this.transitions = transitions;
        this.users = users;
        this.buildings = buildings;
        this.expiry = expiry;
        this.unitOfWork = unitOfWork;
        this.maxDuration = Objects.requireNonNull(maxDuration, "maxDuration");
    }

    @Override
    public SupportSessionDetails execute(Actor actor, StartSupportSessionCommand command) {
        SupportAccess.requireSupportUser(actor);
        var now = expiry.now();
        var session = SupportSession.start(actor.userId(), command.targetUserId(), command.buildingId(),
                command.permissionScope(), command.reason(), command.expiresAt(), now, maxDuration);
        if (session.targetUserId() != null && users.platformRolesOf(session.targetUserId()).isEmpty()) {
            throw SupportErrors.unknownTargetUser(session.targetUserId());
        }
        if (session.buildingId() != null && !buildings.exists(session.buildingId())) {
            throw SupportErrors.unknownBuilding(session.buildingId());
        }
        var requests = session.highRiskScopes().stream()
                .map(scope -> ElevatedApprovalRequest.request(session.id(), scope, now)).toList();
        return unitOfWork.inTransaction(() -> {
            sessions.insert(session);
            transitions.append(LifecycleTransition.of(EntityType.SUPPORT_SESSION, session.id(), null,
                    session.status(), actor.userId(), session.reason(), now));
            for (var request : requests) {
                approvals.insert(request);
                transitions.append(LifecycleTransition.of(EntityType.ELEVATED_APPROVAL_REQUEST, request.id(), null,
                        request.status(), actor.userId(), session.reason(), now));
            }
            return new SupportSessionDetails(session, requests);
        });
    }
}
