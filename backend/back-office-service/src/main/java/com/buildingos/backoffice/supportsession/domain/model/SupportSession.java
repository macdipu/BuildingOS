package com.buildingos.backoffice.supportsession.domain.model;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Reason-bound, scope-limited, time-boxed support access of a platform user to a target user and/or building
 * (BRD §149.12, D-10, D-36). {@code permissionScope} lists every scope named on the session: ordinary scopes are
 * granted directly at start; high-risk scopes are only requested and are usable only while an APPROVED
 * {@link ElevatedApprovalRequest} exists for them on an active session. Immutable: every change returns the next
 * state.
 */
public record SupportSession(UUID id, UUID platformUserId, UUID targetUserId, UUID buildingId, String reason,
        List<SupportScope> permissionScope, Instant startedAt, Instant expiresAt, Instant endedAt) {
    public static final int MAX_REASON_LENGTH = 1000;

    public SupportSession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(platformUserId, "platformUserId");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        permissionScope = List.copyOf(permissionScope);
    }

    /**
     * D-36a/b: the caller opens a session for themselves naming a target user and/or building, a reason, one or more
     * distinct D-10 scopes, and an expiry in the future no later than {@code maxDuration} after the start.
     */
    public static SupportSession start(UUID platformUserId, UUID targetUserId, UUID buildingId,
            List<SupportScope> scopes, String reason, Instant expiresAt, Instant now, Duration maxDuration) {
        Objects.requireNonNull(platformUserId, "platformUserId");
        if (targetUserId == null && buildingId == null) {
            throw SupportSessionRuleException.invalid("targetUserId or buildingId is required");
        }
        List<SupportScope> scope = requireScopes(scopes);
        String auditReason = optionalReason(reason);
        if (auditReason == null) {
            throw SupportSessionRuleException.invalid("reason is required");
        }
        if (expiresAt == null) {
            throw SupportSessionRuleException.invalid("expiresAt is required");
        }
        if (!expiresAt.isAfter(now)) {
            throw SupportSessionRuleException.invalid("expiresAt must be in the future");
        }
        if (expiresAt.isAfter(now.plus(maxDuration))) {
            throw SupportSessionRuleException.invalid("expiresAt must be at most " + maxDuration + " after start");
        }
        return new SupportSession(UUID.randomUUID(), platformUserId, targetUserId, buildingId, auditReason, scope, now,
                expiresAt, null);
    }

    /** Optional audit reason: trimmed, blank = none, at most {@link #MAX_REASON_LENGTH}. */
    public static String optionalReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return null;
        }
        String text = reason.strip();
        if (text.length() > MAX_REASON_LENGTH) {
            throw SupportSessionRuleException.invalid("reason must be at most " + MAX_REASON_LENGTH + " characters");
        }
        return text;
    }

    public SupportSessionStatus status() {
        if (endedAt == null) {
            return SupportSessionStatus.ACTIVE;
        }
        return endedAt.isBefore(expiresAt) ? SupportSessionStatus.ENDED : SupportSessionStatus.EXPIRED;
    }

    public boolean isActive(Instant now) {
        return endedAt == null && now.isBefore(expiresAt);
    }

    /** Not ended and at or past {@code expires_at}: must be recorded as ended automatically. */
    public boolean isDue(Instant now) {
        return endedAt == null && !now.isBefore(expiresAt);
    }

    /** System end at {@code expires_at}; empty when not due. */
    public Optional<SupportSession> expire(Instant now) {
        return isDue(now) ? Optional.of(endedAt(expiresAt)) : Optional.empty();
    }

    /** D-36c: ended by a user now. */
    public SupportSession end(Instant now) {
        requireActive(now);
        return endedAt(now);
    }

    public void requireActive(Instant now) {
        if (!isActive(now)) {
            throw SupportSessionRuleException.ended();
        }
    }

    /** High-risk scopes named on the session; each needs an approval request. */
    public List<SupportScope> highRiskScopes() {
        return permissionScope.stream().filter(SupportScope::isHighRisk).toList();
    }

    /** Records a high-risk scope requested after start on the session's scope list (no-op when already listed). */
    public SupportSession withRequestedScope(SupportScope scope) {
        if (permissionScope.contains(scope)) {
            return this;
        }
        var scopes = new ArrayList<>(permissionScope);
        scopes.add(scope);
        return new SupportSession(id, platformUserId, targetUserId, buildingId, reason,
                List.copyOf(EnumSet.copyOf(scopes)), startedAt, expiresAt, endedAt);
    }

    /**
     * D-36: an ended session allows nothing; an ordinary scope is allowed when granted at start; a high-risk scope is
     * allowed when an approval on this session was approved, pending while one awaits a decision.
     */
    public ScopeCheckResult check(SupportScope scope, Collection<ElevatedApprovalRequest> approvals, Instant now) {
        Objects.requireNonNull(scope, "scope");
        if (!isActive(now)) {
            return ScopeCheckResult.SESSION_ENDED;
        }
        if (!scope.isHighRisk()) {
            return permissionScope.contains(scope) ? ScopeCheckResult.ALLOWED : ScopeCheckResult.NOT_GRANTED;
        }
        var mine = approvals.stream()
                .filter(a -> a.supportSessionId().equals(id) && a.requestedScope() == scope).toList();
        if (mine.stream().anyMatch(a -> a.status() == ElevatedApprovalStatus.APPROVED)) {
            return ScopeCheckResult.ALLOWED;
        }
        if (mine.stream().anyMatch(ElevatedApprovalRequest::isPending)) {
            return ScopeCheckResult.PENDING_APPROVAL;
        }
        return ScopeCheckResult.NOT_GRANTED;
    }

    private SupportSession endedAt(Instant at) {
        return new SupportSession(id, platformUserId, targetUserId, buildingId, reason, permissionScope, startedAt,
                expiresAt, at);
    }

    private static List<SupportScope> requireScopes(List<SupportScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw SupportSessionRuleException.invalid("permissionScope needs at least one support scope");
        }
        if (scopes.stream().anyMatch(Objects::isNull)) {
            throw SupportSessionRuleException.invalid("permissionScope contains an empty value");
        }
        if (EnumSet.copyOf(scopes).size() != scopes.size()) {
            throw SupportSessionRuleException.invalid("permissionScope contains a duplicate scope");
        }
        return List.copyOf(EnumSet.copyOf(scopes));
    }
}
