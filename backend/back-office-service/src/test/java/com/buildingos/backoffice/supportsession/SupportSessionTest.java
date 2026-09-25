package com.buildingos.backoffice.supportsession;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.backoffice.shared.domain.model.DomainRuleException;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalRequest;
import com.buildingos.backoffice.supportsession.domain.model.ElevatedApprovalStatus;
import com.buildingos.backoffice.supportsession.domain.model.ScopeCheckResult;
import com.buildingos.backoffice.supportsession.domain.model.SupportScope;
import com.buildingos.backoffice.supportsession.domain.model.SupportSession;
import com.buildingos.backoffice.supportsession.domain.model.SupportSessionRuleException;
import com.buildingos.backoffice.supportsession.domain.model.SupportSessionStatus;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class SupportSessionTest {
    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");
    private static final Duration MAX = Duration.ofHours(8);
    private final UUID owner = UUID.randomUUID();
    private final UUID target = UUID.randomUUID();

    private SupportSession start(List<SupportScope> scopes) {
        return SupportSession.start(owner, target, null, scopes, " Customer ticket 42 ", NOW.plus(Duration.ofHours(2)),
                NOW, MAX);
    }

    private static void assertInvalid(Runnable action) {
        assertThatThrownBy(action::run).isInstanceOf(SupportSessionRuleException.class)
                .satisfies(e -> assertThat(((DomainRuleException) e).kind()).isEqualTo(DomainRuleException.Kind.INVALID));
    }

    private static void assertConflict(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOf(SupportSessionRuleException.class)
                .satisfies(e -> {
                    assertThat(((DomainRuleException) e).kind()).isEqualTo(DomainRuleException.Kind.CONFLICT);
                    assertThat(((DomainRuleException) e).code()).isEqualTo(code);
                });
    }

    @Test
    void startValidatesTargetReasonScopesAndExpiry() {
        var ok = start(List.of(SupportScope.SUPPORT_VIEW_UNITS));
        assertThat(ok.status()).isEqualTo(SupportSessionStatus.ACTIVE);
        assertThat(ok.reason()).isEqualTo("Customer ticket 42");
        assertThat(ok.platformUserId()).isEqualTo(owner);

        var scopes = List.of(SupportScope.SUPPORT_VIEW_UNITS);
        Instant later = NOW.plus(Duration.ofHours(1));
        assertInvalid(() -> SupportSession.start(owner, null, null, scopes, "r", later, NOW, MAX));
        assertInvalid(() -> SupportSession.start(owner, target, null, List.of(), "r", later, NOW, MAX));
        assertInvalid(() -> SupportSession.start(owner, target, null, null, "r", later, NOW, MAX));
        assertInvalid(() -> SupportSession.start(owner, target, null,
                List.of(SupportScope.SUPPORT_VIEW_UNITS, SupportScope.SUPPORT_VIEW_UNITS), "r", later, NOW, MAX));
        assertInvalid(() -> SupportSession.start(owner, target, null, scopes, "  ", later, NOW, MAX));
        assertInvalid(() -> SupportSession.start(owner, target, null, scopes, "x".repeat(1001), later, NOW, MAX));
        assertInvalid(() -> SupportSession.start(owner, target, null, scopes, "r", null, NOW, MAX));
        assertInvalid(() -> SupportSession.start(owner, target, null, scopes, "r", NOW, NOW, MAX));
        assertInvalid(() -> SupportSession.start(owner, target, null, scopes, "r", NOW.plus(MAX).plusSeconds(1), NOW,
                MAX));
        assertThat(SupportSession.start(owner, null, UUID.randomUUID(), scopes, "r", NOW.plus(MAX), NOW, MAX))
                .isNotNull();
    }

    @Test
    void highRiskScopesAreFlaggedAndOnlyRequestable() {
        assertThat(SupportScope.HIGH_RISK).containsExactlyInAnyOrder(SupportScope.SUPPORT_REVERSE_PAYMENT,
                SupportScope.SUPPORT_TRANSFER_OWNERSHIP, SupportScope.SUPPORT_REMOVE_BUILDING_ADMIN,
                SupportScope.SUPPORT_EXPORT_FINANCIAL_UNRESTRICTED);
        assertThat(SupportScope.SUPPORT_VIEW_PAYMENTS.isHighRisk()).isFalse();
        var session = start(List.of(SupportScope.SUPPORT_VIEW_UNITS, SupportScope.SUPPORT_TRANSFER_OWNERSHIP));
        assertThat(session.highRiskScopes()).containsExactly(SupportScope.SUPPORT_TRANSFER_OWNERSHIP);
        assertInvalid(() -> ElevatedApprovalRequest.request(session.id(), SupportScope.SUPPORT_VIEW_UNITS, NOW));
    }

    @Test
    void scopeCheckFollowsGrantsApprovalsAndSessionEnd() {
        var session = start(List.of(SupportScope.SUPPORT_VIEW_UNITS, SupportScope.SUPPORT_TRANSFER_OWNERSHIP));
        var pending = ElevatedApprovalRequest.request(session.id(), SupportScope.SUPPORT_TRANSFER_OWNERSHIP, NOW);
        assertThat(session.check(SupportScope.SUPPORT_VIEW_UNITS, List.of(), NOW)).isEqualTo(ScopeCheckResult.ALLOWED);
        assertThat(session.check(SupportScope.SUPPORT_EDIT_UNIT, List.of(), NOW))
                .isEqualTo(ScopeCheckResult.NOT_GRANTED);
        assertThat(session.check(SupportScope.SUPPORT_TRANSFER_OWNERSHIP, List.of(pending), NOW))
                .isEqualTo(ScopeCheckResult.PENDING_APPROVAL);
        var approved = pending.approve(UUID.randomUUID(), null, NOW);
        assertThat(session.check(SupportScope.SUPPORT_TRANSFER_OWNERSHIP, List.of(approved), NOW))
                .isEqualTo(ScopeCheckResult.ALLOWED);
        assertThat(session.check(SupportScope.SUPPORT_REVERSE_PAYMENT, List.of(approved), NOW))
                .isEqualTo(ScopeCheckResult.NOT_GRANTED);
        var denied = ElevatedApprovalRequest.request(session.id(), SupportScope.SUPPORT_TRANSFER_OWNERSHIP, NOW)
                .deny(UUID.randomUUID(), "no", NOW);
        assertThat(session.check(SupportScope.SUPPORT_TRANSFER_OWNERSHIP, List.of(denied), NOW))
                .isEqualTo(ScopeCheckResult.NOT_GRANTED);
        Instant past = session.expiresAt();
        assertThat(session.check(SupportScope.SUPPORT_TRANSFER_OWNERSHIP, List.of(approved), past))
                .isEqualTo(ScopeCheckResult.SESSION_ENDED);
        var ended = session.end(NOW.plusSeconds(60));
        assertThat(ended.status()).isEqualTo(SupportSessionStatus.ENDED);
        assertThat(ended.check(SupportScope.SUPPORT_VIEW_UNITS, List.of(), NOW.plusSeconds(61)))
                .isEqualTo(ScopeCheckResult.SESSION_ENDED);
    }

    @Test
    void endAndExpiry() {
        var session = start(List.of(SupportScope.SUPPORT_VIEW_UNITS));
        assertThat(session.expire(NOW)).isEmpty();
        var expired = session.expire(session.expiresAt()).orElseThrow();
        assertThat(expired.status()).isEqualTo(SupportSessionStatus.EXPIRED);
        assertThat(expired.endedAt()).isEqualTo(session.expiresAt());
        assertConflict(() -> expired.end(session.expiresAt().plusSeconds(1)), "SESSION_ENDED");
        assertConflict(() -> session.end(session.expiresAt()), "SESSION_ENDED");
        var ended = session.end(NOW.plusSeconds(1));
        assertConflict(() -> ended.end(NOW.plusSeconds(2)), "SESSION_ENDED");
        assertThat(ended.expire(session.expiresAt())).isEmpty();
    }

    @Test
    void decisionsNeedPendingAndDenyNeedsReason() {
        var request = ElevatedApprovalRequest.request(UUID.randomUUID(), SupportScope.SUPPORT_REVERSE_PAYMENT, NOW);
        assertThat(request.status()).isEqualTo(ElevatedApprovalStatus.PENDING);
        UUID approver = UUID.randomUUID();
        assertInvalid(() -> request.deny(approver, " ", NOW));
        var denied = request.deny(approver, "Not justified", NOW);
        assertThat(denied.status()).isEqualTo(ElevatedApprovalStatus.DENIED);
        assertThat(denied.decidedBy()).isEqualTo(approver);
        assertThat(denied.decisionReason()).isEqualTo("Not justified");
        assertConflict(() -> denied.approve(approver, null, NOW), "APPROVAL_NOT_PENDING");
        var approved = request.approve(approver, null, NOW);
        assertThat(approved.decidedAt()).isEqualTo(NOW);
        assertConflict(() -> approved.deny(approver, "x", NOW), "APPROVAL_NOT_PENDING");
    }

    @Test
    void laterRequestedScopeIsRecordedOnce() {
        var session = start(List.of(SupportScope.SUPPORT_VIEW_UNITS));
        var next = session.withRequestedScope(SupportScope.SUPPORT_REVERSE_PAYMENT);
        assertThat(next.permissionScope()).containsExactly(SupportScope.SUPPORT_VIEW_UNITS,
                SupportScope.SUPPORT_REVERSE_PAYMENT);
        assertThat(next.withRequestedScope(SupportScope.SUPPORT_REVERSE_PAYMENT)).isSameAs(next);
    }
}
