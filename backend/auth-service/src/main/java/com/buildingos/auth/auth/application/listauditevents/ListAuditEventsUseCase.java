package com.buildingos.auth.auth.application.listauditevents;

import java.util.List;

public interface ListAuditEventsUseCase {
    List<AuditEvent> execute(ListAuditEventsQuery query);
}
