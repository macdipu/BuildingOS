package com.buildingos.backoffice.onboarding.application.startonboardingsession;

import com.buildingos.backoffice.onboarding.application.OnboardingErrors;
import com.buildingos.backoffice.onboarding.application.OnboardingSessionExpiry;
import com.buildingos.backoffice.shared.application.port.out.BuildingDirectory;
import com.buildingos.backoffice.shared.application.port.out.PlatformUserDirectory;
import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.onboarding.domain.repository.AssistedOnboardingSessionRepository;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.port.out.UnitOfWork;
import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Duration;
import java.util.Objects;

/**
 * D-33a-c: a SUPER_ADMIN/PLATFORM_ADMIN assigns an agent who currently holds ONBOARDING_AGENT to an existing building.
 * Both dependencies are checked before anything is stored, so a dependency failure (503) leaves no row behind.
 */
public final class StartOnboardingSessionService implements StartOnboardingSessionUseCase {
    private final AssistedOnboardingSessionRepository sessions;
    private final LifecycleTransitionRepository transitions;
    private final PlatformUserDirectory users;
    private final BuildingDirectory buildings;
    private final OnboardingSessionExpiry expiry;
    private final UnitOfWork unitOfWork;
    private final Duration maxDuration;

    public StartOnboardingSessionService(AssistedOnboardingSessionRepository sessions,
            LifecycleTransitionRepository transitions, PlatformUserDirectory users, BuildingDirectory buildings,
            OnboardingSessionExpiry expiry, UnitOfWork unitOfWork, Duration maxDuration) {
        this.sessions = sessions;
        this.transitions = transitions;
        this.users = users;
        this.buildings = buildings;
        this.expiry = expiry;
        this.unitOfWork = unitOfWork;
        this.maxDuration = Objects.requireNonNull(maxDuration, "maxDuration");
    }

    @Override
    public AssistedOnboardingSession execute(Actor actor, StartOnboardingSessionCommand command) {
        actor.requireOperator();
        var now = expiry.now();
        var session = AssistedOnboardingSession.assign(command.buildingId(), command.assignedAgentUserId(),
                command.accessScope(), command.reason(), command.notes(), command.expiresAt(), now, maxDuration);
        boolean agent = users.platformRolesOf(session.assignedAgentUserId())
                .map(roles -> roles.contains(Actor.ONBOARDING_AGENT)).orElse(false);
        if (!agent) {
            throw OnboardingErrors.assigneeNotAgent(session.assignedAgentUserId());
        }
        if (!buildings.exists(session.buildingId())) {
            throw OnboardingErrors.unknownBuilding(session.buildingId());
        }
        return unitOfWork.inTransaction(() -> {
            sessions.insert(session);
            transitions.append(LifecycleTransition.of(EntityType.ASSISTED_ONBOARDING_SESSION, session.id(), null,
                    session.status(), actor.userId(), session.reason(), now));
            return session;
        });
    }
}
