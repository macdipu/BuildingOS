package com.buildingos.backoffice.onboarding;

import static com.buildingos.backoffice.onboarding.domain.model.OnboardingScope.ONBOARDING_MANAGE_FLOORS_UNITS;
import static com.buildingos.backoffice.onboarding.domain.model.OnboardingScope.ONBOARDING_SEND_INVITATIONS;
import static com.buildingos.backoffice.onboarding.domain.model.OnboardingScope.ONBOARDING_VIEW_BUILDING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.onboarding.domain.model.OnboardingScope;
import com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionRuleException;
import com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionStatus;
import com.buildingos.backoffice.shared.domain.model.DomainRuleException;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AssistedOnboardingSessionTest {
    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");
    private static final Duration MAX = Duration.ofDays(30);
    private static final UUID BUILDING = UUID.randomUUID();
    private static final UUID AGENT = UUID.randomUUID();

    private static AssistedOnboardingSession assigned() {
        return AssistedOnboardingSession.assign(BUILDING, AGENT,
                List.of(ONBOARDING_SEND_INVITATIONS, ONBOARDING_MANAGE_FLOORS_UNITS), "  Needs help with 60 units ",
                null, NOW.plus(Duration.ofDays(7)), NOW, MAX);
    }

    private static void assertRule(Runnable action, String code, DomainRuleException.Kind kind) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(OnboardingSessionRuleException.class, e -> {
            assertThat(e.code()).isEqualTo(code);
            assertThat(e.kind()).isEqualTo(kind);
        });
    }

    private static void assertInvalid(Runnable action) {
        assertRule(action, "INVALID_REQUEST", DomainRuleException.Kind.INVALID);
    }

    @Test
    void assignCreatesAnAssignedSessionStartedNow() {
        var session = assigned();
        assertThat(session.status()).isEqualTo(OnboardingSessionStatus.ASSIGNED);
        assertThat(session.startedAt()).isEqualTo(NOW);
        assertThat(session.reason()).isEqualTo("Needs help with 60 units");
        assertThat(session.accessScope()).containsExactly(ONBOARDING_MANAGE_FLOORS_UNITS, ONBOARDING_SEND_INVITATIONS);
        assertThat(session.requestedByUserId()).isNull();
        assertThat(session.completedAt()).isNull();
    }

    @Test
    void everyD10OnboardingScopeIsAccepted() {
        var session = AssistedOnboardingSession.assign(BUILDING, AGENT, Arrays.asList(OnboardingScope.values()),
                "r", null, NOW.plusSeconds(60), NOW, MAX);
        assertThat(session.accessScope()).hasSize(9);
    }

    @Test
    void scopesMustBeNonEmptyAndDistinct() {
        assertInvalid(() -> AssistedOnboardingSession.assign(BUILDING, AGENT, List.of(), "r", null,
                NOW.plusSeconds(60), NOW, MAX));
        assertInvalid(() -> AssistedOnboardingSession.assign(BUILDING, AGENT, null, "r", null,
                NOW.plusSeconds(60), NOW, MAX));
        assertInvalid(() -> AssistedOnboardingSession.assign(BUILDING, AGENT,
                List.of(ONBOARDING_VIEW_BUILDING, ONBOARDING_VIEW_BUILDING), "r", null, NOW.plusSeconds(60), NOW, MAX));
    }

    @Test
    void reasonIsRequiredAndBounded() {
        assertInvalid(() -> AssistedOnboardingSession.assign(BUILDING, AGENT, List.of(ONBOARDING_VIEW_BUILDING),
                "   ", null, NOW.plusSeconds(60), NOW, MAX));
        assertInvalid(() -> AssistedOnboardingSession.assign(BUILDING, AGENT, List.of(ONBOARDING_VIEW_BUILDING),
                "x".repeat(1001), null, NOW.plusSeconds(60), NOW, MAX));
    }

    @Test
    void expiryMustBeFutureAndWithinMaxDuration() {
        List<OnboardingScope> scope = List.of(ONBOARDING_VIEW_BUILDING);
        assertInvalid(() -> AssistedOnboardingSession.assign(BUILDING, AGENT, scope, "r", null, NOW, NOW, MAX));
        assertInvalid(() -> AssistedOnboardingSession.assign(BUILDING, AGENT, scope, "r", null,
                NOW.minusSeconds(1), NOW, MAX));
        assertInvalid(() -> AssistedOnboardingSession.assign(BUILDING, AGENT, scope, "r", null,
                NOW.plus(MAX).plusSeconds(1), NOW, MAX));
        assertInvalid(() -> AssistedOnboardingSession.assign(BUILDING, AGENT, scope, "r", null, null, NOW, MAX));
        assertThat(AssistedOnboardingSession.assign(BUILDING, AGENT, scope, "r", null, NOW.plus(MAX), NOW, MAX)
                .expiresAt()).isEqualTo(NOW.plus(MAX));
    }

    @Test
    void buildingAndAgentAreRequired() {
        List<OnboardingScope> scope = List.of(ONBOARDING_VIEW_BUILDING);
        assertInvalid(() -> AssistedOnboardingSession.assign(null, AGENT, scope, "r", null, NOW.plusSeconds(9), NOW,
                MAX));
        assertInvalid(() -> AssistedOnboardingSession.assign(BUILDING, null, scope, "r", null, NOW.plusSeconds(9),
                NOW, MAX));
    }

    @Test
    void agentWorkflowMovesBetweenInProgressAndWaiting() {
        var working = assigned().startWork(NOW.plusSeconds(1));
        assertThat(working.status()).isEqualTo(OnboardingSessionStatus.IN_PROGRESS);
        var waiting = working.awaitCustomer(NOW.plusSeconds(2));
        assertThat(waiting.status()).isEqualTo(OnboardingSessionStatus.WAITING_FOR_CUSTOMER);
        assertThat(waiting.startWork(NOW.plusSeconds(3)).status()).isEqualTo(OnboardingSessionStatus.IN_PROGRESS);
    }

    @Test
    void invalidAgentTransitionsConflict() {
        var session = assigned();
        assertRule(() -> session.awaitCustomer(NOW), "INVALID_TRANSITION", DomainRuleException.Kind.CONFLICT);
        var working = session.startWork(NOW);
        assertRule(() -> working.startWork(NOW), "INVALID_TRANSITION", DomainRuleException.Kind.CONFLICT);
    }

    @Test
    void completeAndCancelEndTheSession() {
        Instant at = NOW.plusSeconds(5);
        var completed = assigned().startWork(NOW).complete(at);
        assertThat(completed.status()).isEqualTo(OnboardingSessionStatus.COMPLETED);
        assertThat(completed.completedAt()).isEqualTo(at);
        var cancelled = assigned().cancel(at);
        assertThat(cancelled.status()).isEqualTo(OnboardingSessionStatus.CANCELLED);
        assertThat(cancelled.completedAt()).isNull();
        assertThat(assigned().complete(at).status()).isEqualTo(OnboardingSessionStatus.COMPLETED);
    }

    @Test
    void endedSessionsRejectEveryChange() {
        for (var ended : List.of(assigned().complete(NOW), assigned().cancel(NOW))) {
            assertRule(() -> ended.startWork(NOW), "SESSION_ENDED", DomainRuleException.Kind.CONFLICT);
            assertRule(() -> ended.awaitCustomer(NOW), "SESSION_ENDED", DomainRuleException.Kind.CONFLICT);
            assertRule(() -> ended.complete(NOW), "SESSION_ENDED", DomainRuleException.Kind.CONFLICT);
            assertRule(() -> ended.cancel(NOW), "SESSION_ENDED", DomainRuleException.Kind.CONFLICT);
            assertThat(ended.expire(NOW.plus(MAX))).isEmpty();
        }
    }

    @Test
    void activeSessionExpiresAtOrAfterExpiresAt() {
        var session = assigned().startWork(NOW);
        assertThat(session.expire(session.expiresAt().minusNanos(1000))).isEmpty();
        var expired = session.expire(session.expiresAt()).orElseThrow();
        assertThat(expired.status()).isEqualTo(OnboardingSessionStatus.EXPIRED);
        assertRule(() -> expired.complete(NOW), "SESSION_ENDED", DomainRuleException.Kind.CONFLICT);
        assertThat(expired.expire(expired.expiresAt().plusSeconds(1))).isEmpty();
    }

    @Test
    void sessionPastExpiryCannotChangeEvenBeforeItIsPersistedExpired() {
        var session = assigned();
        Instant late = session.expiresAt().plusSeconds(1);
        assertThat(session.isDue(late)).isTrue();
        assertRule(() -> session.startWork(late), "SESSION_ENDED", DomainRuleException.Kind.CONFLICT);
        assertRule(() -> session.cancel(late), "SESSION_ENDED", DomainRuleException.Kind.CONFLICT);
    }

    @Test
    void transitionReasonIsOptionalButBounded() {
        assertThat(AssistedOnboardingSession.transitionReason(null)).isNull();
        assertThat(AssistedOnboardingSession.transitionReason(" done ")).isEqualTo("done");
        assertInvalid(() -> AssistedOnboardingSession.transitionReason("x".repeat(1001)));
    }
}
