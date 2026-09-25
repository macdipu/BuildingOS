package com.buildingos.backoffice.auditlog.application.port.out;

import com.buildingos.backoffice.auditlog.domain.model.AuditEvent;
import com.buildingos.backoffice.auditlog.domain.model.AuditFilter;
import com.buildingos.backoffice.auditlog.domain.model.AuditSource;
import java.util.List;

/**
 * Another service's {@code GET /internal/audit} (F6-T5b/F6-T5c contract), called with the caller's relayed token.
 * Throws a runtime exception when the source errors, answers non-2xx or times out.
 */
public interface RemoteAuditLog {
    List<AuditEvent> fetch(AuditSource source, AuditFilter filter);
}
