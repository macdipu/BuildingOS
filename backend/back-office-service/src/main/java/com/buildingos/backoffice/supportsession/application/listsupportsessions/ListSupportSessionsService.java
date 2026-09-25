package com.buildingos.backoffice.supportsession.application.listsupportsessions;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.Page;
import com.buildingos.backoffice.supportsession.application.SupportAccess;
import com.buildingos.backoffice.supportsession.application.SupportSessionExpiry;
import com.buildingos.backoffice.supportsession.domain.model.SupportSession;
import com.buildingos.backoffice.supportsession.domain.model.SupportSessionFilter;
import com.buildingos.backoffice.supportsession.domain.repository.SupportSessionRepository;
import java.util.List;

/** Admins list every session (Support History); a support agent only their own (another owner filter yields none). */
public final class ListSupportSessionsService implements ListSupportSessionsUseCase {
    private final SupportSessionRepository sessions;
    private final SupportSessionExpiry expiry;

    public ListSupportSessionsService(SupportSessionRepository sessions, SupportSessionExpiry expiry) {
        this.sessions = sessions;
        this.expiry = expiry;
    }

    @Override
    public Page<SupportSession> execute(Actor actor, ListSupportSessionsQuery query) {
        SupportAccess.requireSupportUser(actor);
        Page.validate(query.page(), query.size());
        var owner = query.platformUserId();
        if (!actor.isOperator()) {
            if (owner != null && !owner.equals(actor.userId())) {
                return new Page<>(List.of(), query.page(), query.size(), 0);
            }
            owner = actor.userId();
        }
        expiry.expireAllDue();
        var filter = new SupportSessionFilter(query.active(), owner, query.targetUserId(), query.buildingId());
        var items = sessions.search(filter, query.page() * query.size(), query.size());
        return new Page<>(items, query.page(), query.size(), sessions.count(filter));
    }
}
