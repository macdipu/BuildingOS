package com.buildingos.auth.auth.application.listauditevents;

import com.buildingos.auth.auth.application.PlatformRoleManagementNotPermittedException;
import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.model.PlatformRoleAuditAction;
import com.buildingos.auth.auth.domain.model.PlatformRoleAuditEntry;
import com.buildingos.auth.auth.domain.repository.PlatformRoleAuditRepository;
import java.util.List;

/** SUPER_ADMIN only; platform-role grant/revoke rows (D-37), newest first. */
public final class ListAuditEventsService implements ListAuditEventsUseCase {
    public static final String SOURCE = "auth-service";
    public static final String ENTITY_TYPE = "PLATFORM_ROLE";
    public static final int DEFAULT_LIMIT = 50;
    public static final int MAX_LIMIT = 200;
    private final PlatformRoleAuditRepository audits;

    public ListAuditEventsService(PlatformRoleAuditRepository audits) {
        this.audits = audits;
    }

    @Override
    public List<AuditEvent> execute(ListAuditEventsQuery query) {
        if (!query.callerPlatformRoles().contains(PlatformRole.SUPER_ADMIN.name())) {
            throw new PlatformRoleManagementNotPermittedException();
        }
        int limit = query.limit() == null ? DEFAULT_LIMIT : query.limit();
        if (limit < 1 || limit > MAX_LIMIT) {
            throw new IllegalArgumentException("limit must be 1-" + MAX_LIMIT);
        }
        if (query.entityType() != null && !ENTITY_TYPE.equals(query.entityType())) {
            return List.of();
        }
        return audits.list(query.since(), query.until(), query.actorUserId(), limit).stream()
                .map(ListAuditEventsService::toEvent)
                .toList();
    }

    private static AuditEvent toEvent(PlatformRoleAuditEntry entry) {
        String action = entry.action() == PlatformRoleAuditAction.GRANT
                ? "PLATFORM_ROLE_GRANTED" : "PLATFORM_ROLE_REVOKED";
        return new AuditEvent(entry.id(), SOURCE, entry.occurredAt(), entry.actorUserId(), action, ENTITY_TYPE,
                entry.targetUserId(), null, null, entry.role().name());
    }
}
