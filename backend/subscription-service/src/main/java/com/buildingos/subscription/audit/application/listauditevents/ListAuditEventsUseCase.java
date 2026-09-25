package com.buildingos.subscription.audit.application.listauditevents;

import com.buildingos.subscription.shared.application.Actor;
import java.util.List;

public interface ListAuditEventsUseCase {
    List<AuditEvent> execute(Actor actor, ListAuditEventsQuery query);
}
