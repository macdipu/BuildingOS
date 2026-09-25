package com.buildingos.backoffice.onboarding.domain.model;

import static com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionStatus.ASSIGNED;
import static com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionStatus.CANCELLED;
import static com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionStatus.COMPLETED;
import static com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionStatus.EXPIRED;
import static com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionStatus.IN_PROGRESS;
import static com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionStatus.WAITING_FOR_CUSTOMER;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

/**
 * Temporary, building- and scope-limited assignment of an {@code ONBOARDING_AGENT} (BRD §149.11, D-10, D-33).
 * Immutable: every transition returns the next state.
 */
public record AssistedOnboardingSession(UUID id, UUID buildingId, UUID assignedAgentUserId, UUID requestedByUserId,
        OnboardingSessionStatus status, List<OnboardingScope> accessScope, String reason, Instant startedAt,
        Instant expiresAt, Instant completedAt, String notes) {
    public static final int MAX_REASON_LENGTH = 1000;
    public static final int MAX_NOTES_LENGTH = 2000;

    public AssistedOnboardingSession {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(buildingId, "buildingId");
        Objects.requireNonNull(assignedAgentUserId, "assignedAgentUserId");
        Objects.requireNonNull(status, "status");
        Objects.requireNonNull(reason, "reason");
        Objects.requireNonNull(startedAt, "startedAt");
        Objects.requireNonNull(expiresAt, "expiresAt");
        accessScope = List.copyOf(accessScope);
    }

    /**
     * D-33: an operator assigns an agent directly ({@code ASSIGNED}, started now). One or more distinct D-10 scopes,
     * a reason, and an expiry in the future no later than {@code maxDuration} after the start.
     */
    public static AssistedOnboardingSession assign(UUID buildingId, UUID agentUserId, List<OnboardingScope> scopes,
            String reason, String notes, Instant expiresAt, Instant now, Duration maxDuration) {
        if (buildingId == null) {
            throw OnboardingSessionRuleException.invalid("buildingId is required");
        }
        if (agentUserId == null) {
            throw OnboardingSessionRuleException.invalid("assignedAgentUserId is required");
        }
        List<OnboardingScope> scope = requireScopes(scopes);
        String auditReason = requireText(reason, "reason", MAX_REASON_LENGTH);
        String note = optionalText(notes, "notes", MAX_NOTES_LENGTH);
        if (expiresAt == null) {
            throw OnboardingSessionRuleException.invalid("expiresAt is required");
        }
        if (!expiresAt.isAfter(now)) {
            throw OnboardingSessionRuleException.invalid("expiresAt must be in the future");
        }
        if (expiresAt.isAfter(now.plus(maxDuration))) {
            throw OnboardingSessionRuleException.invalid("expiresAt must be at most " + maxDuration + " after start");
        }
        return new AssistedOnboardingSession(UUID.randomUUID(), buildingId, agentUserId, null, ASSIGNED, scope,
                auditReason, now, expiresAt, null, note);
    }

    /** Audit reason for a transition: optional, trimmed, at most {@link #MAX_REASON_LENGTH}. */
    public static String transitionReason(String reason) {
        return optionalText(reason, "reason", MAX_REASON_LENGTH);
    }

    /** Active and at or past {@code expires_at}: access has lapsed and the session must be recorded EXPIRED. */
    public boolean isDue(Instant now) {
        return status.isActive() && !now.isBefore(expiresAt);
    }

    /** System transition; empty when the session is not due. */
    public Optional<AssistedOnboardingSession> expire(Instant now) {
        return isDue(now) ? Optional.of(with(EXPIRED, completedAt)) : Optional.empty();
    }

    /** Agent: {@code ASSIGNED | WAITING_FOR_CUSTOMER -> IN_PROGRESS}. */
    public AssistedOnboardingSession startWork(Instant now) {
        requireActive(now);
        if (status != ASSIGNED && status != WAITING_FOR_CUSTOMER) {
            throw OnboardingSessionRuleException.invalidTransition(status, "start work on");
        }
        return with(IN_PROGRESS, completedAt);
    }

    /** Agent: {@code IN_PROGRESS -> WAITING_FOR_CUSTOMER}. */
    public AssistedOnboardingSession awaitCustomer(Instant now) {
        requireActive(now);
        if (status != IN_PROGRESS) {
            throw OnboardingSessionRuleException.invalidTransition(status, "await the customer on");
        }
        return with(WAITING_FOR_CUSTOMER, completedAt);
    }

    /** Assigned agent or operator: any assigned/active status to {@code COMPLETED}. */
    public AssistedOnboardingSession complete(Instant now) {
        requireAssignedActive(now, "complete");
        return with(COMPLETED, now);
    }

    /** Operator: any assigned/active status to {@code CANCELLED}. */
    public AssistedOnboardingSession cancel(Instant now) {
        requireAssignedActive(now, "cancel");
        return with(CANCELLED, completedAt);
    }

    private void requireAssignedActive(Instant now, String action) {
        requireActive(now);
        if (!EnumSet.of(ASSIGNED, IN_PROGRESS, WAITING_FOR_CUSTOMER).contains(status)) {
            throw OnboardingSessionRuleException.invalidTransition(status, action);
        }
    }

    private void requireActive(Instant now) {
        if (!status.isActive()) {
            throw OnboardingSessionRuleException.ended(status);
        }
        if (isDue(now)) {
            throw OnboardingSessionRuleException.ended(EXPIRED);
        }
    }

    private AssistedOnboardingSession with(OnboardingSessionStatus next, Instant completed) {
        return new AssistedOnboardingSession(id, buildingId, assignedAgentUserId, requestedByUserId, next, accessScope,
                reason, startedAt, expiresAt, completed, notes);
    }

    private static List<OnboardingScope> requireScopes(List<OnboardingScope> scopes) {
        if (scopes == null || scopes.isEmpty()) {
            throw OnboardingSessionRuleException.invalid("accessScope needs at least one onboarding scope");
        }
        if (scopes.stream().anyMatch(Objects::isNull)) {
            throw OnboardingSessionRuleException.invalid("accessScope contains an empty value");
        }
        if (EnumSet.copyOf(scopes).size() != scopes.size()) {
            throw OnboardingSessionRuleException.invalid("accessScope contains a duplicate scope");
        }
        return List.copyOf(EnumSet.copyOf(scopes));
    }

    private static String requireText(String value, String field, int max) {
        String text = optionalText(value, field, max);
        if (text == null) {
            throw OnboardingSessionRuleException.invalid(field + " is required");
        }
        return text;
    }

    private static String optionalText(String value, String field, int max) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String text = value.strip();
        if (text.length() > max) {
            throw OnboardingSessionRuleException.invalid(field + " must be at most " + max + " characters");
        }
        return text;
    }
}
