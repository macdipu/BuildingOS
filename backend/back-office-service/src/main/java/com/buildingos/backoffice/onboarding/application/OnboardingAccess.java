package com.buildingos.backoffice.onboarding.application;

import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.NotPermittedException;

/** D-33 visibility: operators see every session, an agent only the sessions assigned to them, anyone else nothing. */
public final class OnboardingAccess {
    /** Who may perform a session transition. */
    public enum Permission { ASSIGNED_AGENT, ASSIGNED_AGENT_OR_OPERATOR, OPERATOR }

    private OnboardingAccess() {}

    public static void requireViewer(Actor actor) {
        if (!actor.isOperator() && !actor.isOnboardingAgent()) {
            throw new NotPermittedException();
        }
    }

    public static boolean canSee(Actor actor, AssistedOnboardingSession session) {
        return actor.isOperator() || isAssignedAgent(actor, session);
    }

    /** The assigned agent, still holding {@code ONBOARDING_AGENT} according to the access token. */
    public static boolean isAssignedAgent(Actor actor, AssistedOnboardingSession session) {
        return actor.isOnboardingAgent() && actor.userId().equals(session.assignedAgentUserId());
    }

    public static boolean permits(Permission permission, Actor actor, AssistedOnboardingSession session) {
        return switch (permission) {
            case ASSIGNED_AGENT -> isAssignedAgent(actor, session);
            case ASSIGNED_AGENT_OR_OPERATOR -> isAssignedAgent(actor, session) || actor.isOperator();
            case OPERATOR -> actor.isOperator();
        };
    }
}
