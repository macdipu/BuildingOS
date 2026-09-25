package com.buildingos.building.audit.application.listauditevents;

import com.buildingos.building.shared.application.Actor;
import java.util.List;

public interface ListAuditEventsUseCase {
    List<AuditEvent> execute(Actor actor, ListAuditEventsQuery query);
}
