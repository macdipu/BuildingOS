package com.buildingos.backoffice.supportsession.application.listsupportsessions;

import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.Page;
import com.buildingos.backoffice.supportsession.domain.model.SupportSession;

public interface ListSupportSessionsUseCase {
    Page<SupportSession> execute(Actor actor, ListSupportSessionsQuery input);
}
