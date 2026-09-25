package com.buildingos.backoffice.auditlog.application.listauditevents;

import com.buildingos.backoffice.auditlog.domain.model.AuditPage;
import com.buildingos.backoffice.shared.application.Actor;

public interface ListAuditEventsUseCase {
    AuditPage execute(Actor actor, ListAuditEventsQuery query);
}
