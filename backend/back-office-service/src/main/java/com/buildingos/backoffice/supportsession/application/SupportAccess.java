package com.buildingos.backoffice.supportsession.application;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.NotPermittedException;
import com.buildingos.backoffice.supportsession.domain.model.SupportSession;

/**
 * D-36a/c: SUPPORT_AGENT, PLATFORM_ADMIN and SUPER_ADMIN may use support sessions; admins see every session, a
 * support agent only their own; anyone else nothing.
 */
public final class SupportAccess {
    private SupportAccess() {}

    public static void requireSupportUser(Actor actor) {
        if (!actor.isOperator() && !actor.isSupportAgent()) {
            throw new NotPermittedException();
        }
    }

    public static boolean canSee(Actor actor, SupportSession session) {
        return actor.isOperator() || (actor.isSupportAgent() && isOwner(actor, session));
    }

    public static boolean isOwner(Actor actor, SupportSession session) {
        return actor.userId().equals(session.platformUserId());
    }

    /** D-36c: the owner, SUPER_ADMIN or PLATFORM_ADMIN. */
    public static boolean mayEnd(Actor actor, SupportSession session) {
        return actor.isOperator() || isOwner(actor, session);
    }

    /** D-36d four-eyes: a SUPER_ADMIN other than the session owner. */
    public static boolean mayDecide(Actor actor, SupportSession session) {
        return actor.isSuperAdmin() && !isOwner(actor, session);
    }
}
