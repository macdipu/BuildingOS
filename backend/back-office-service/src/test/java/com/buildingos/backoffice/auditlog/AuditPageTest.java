package com.buildingos.backoffice.auditlog;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.backoffice.auditlog.domain.model.AuditEvent;
import com.buildingos.backoffice.auditlog.domain.model.AuditPage;
import com.buildingos.backoffice.auditlog.domain.model.AuditSource;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** F6-T5d: merge newest first, truncate to limit, cursor = oldest returned occurredAt only when full. */
class AuditPageTest {
    private static AuditEvent event(AuditSource source, String id, String at) {
        return new AuditEvent(UUID.fromString(id), source, Instant.parse(at), null, "A", "X", "e", null, null, null);
    }

    private static final AuditEvent AUTH_1 = event(AuditSource.AUTH_SERVICE,
            "00000000-0000-0000-0000-000000000001", "2026-09-25T10:00:03Z");
    private static final AuditEvent AUTH_2 = event(AuditSource.AUTH_SERVICE,
            "00000000-0000-0000-0000-000000000002", "2026-09-25T10:00:01Z");
    private static final AuditEvent BUILDING_1 = event(AuditSource.BUILDING_SERVICE,
            "00000000-0000-0000-0000-000000000003", "2026-09-25T10:00:02Z");
    /** Same instant as BUILDING_1, higher id: sorts first. */
    private static final AuditEvent OWN_1 = event(AuditSource.BACK_OFFICE_SERVICE,
            "00000000-0000-0000-0000-000000000004", "2026-09-25T10:00:02Z");

    @Test
    void mergesNewestFirstThenIdDescending() {
        var page = AuditPage.merge(List.of(List.of(AUTH_1, AUTH_2), List.of(BUILDING_1), List.of(OWN_1)), 10,
                List.of());
        assertThat(page.items()).containsExactly(AUTH_1, OWN_1, BUILDING_1, AUTH_2);
        assertThat(page.nextUntil()).isNull();
    }

    @Test
    void truncatesToLimitAndSetsCursorToOldestReturned() {
        var page = AuditPage.merge(List.of(List.of(AUTH_1, AUTH_2), List.of(BUILDING_1), List.of(OWN_1)), 2,
                List.of(AuditSource.SUBSCRIPTION_SERVICE));
        assertThat(page.items()).containsExactly(AUTH_1, OWN_1);
        assertThat(page.nextUntil()).isEqualTo(OWN_1.occurredAt());
        assertThat(page.unavailableSources()).containsExactly(AuditSource.SUBSCRIPTION_SERVICE);
    }

    @Test
    void exactlyLimitItemsStillGivesCursorAndEmptyGivesNone() {
        assertThat(AuditPage.merge(List.of(List.of(AUTH_1, AUTH_2)), 2, List.of()).nextUntil())
                .isEqualTo(AUTH_2.occurredAt());
        var empty = AuditPage.merge(List.of(), 5, List.of());
        assertThat(empty.items()).isEmpty();
        assertThat(empty.nextUntil()).isNull();
    }
}
