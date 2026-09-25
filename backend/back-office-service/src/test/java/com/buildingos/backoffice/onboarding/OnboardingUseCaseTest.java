package com.buildingos.backoffice.onboarding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.backoffice.onboarding.application.OnboardingSessionChanges;
import com.buildingos.backoffice.onboarding.application.OnboardingSessionExpiry;
import com.buildingos.backoffice.onboarding.application.OnboardingTransitionCommand;
import com.buildingos.backoffice.onboarding.application.cancelonboardingsession.CancelOnboardingSessionService;
import com.buildingos.backoffice.onboarding.application.completeonboardingsession.CompleteOnboardingSessionService;
import com.buildingos.backoffice.onboarding.application.getonboardingsession.GetOnboardingSessionQuery;
import com.buildingos.backoffice.onboarding.application.getonboardingsession.GetOnboardingSessionService;
import com.buildingos.backoffice.onboarding.application.listonboardingsessions.ListOnboardingSessionsQuery;
import com.buildingos.backoffice.onboarding.application.listonboardingsessions.ListOnboardingSessionsService;
import com.buildingos.backoffice.shared.application.port.out.BuildingDirectory;
import com.buildingos.backoffice.shared.application.port.out.PlatformUserDirectory;
import com.buildingos.backoffice.onboarding.application.startonboardingsession.StartOnboardingSessionCommand;
import com.buildingos.backoffice.onboarding.application.startonboardingsession.StartOnboardingSessionService;
import com.buildingos.backoffice.onboarding.application.startonboardingwork.StartOnboardingWorkService;
import com.buildingos.backoffice.onboarding.domain.model.AssistedOnboardingSession;
import com.buildingos.backoffice.onboarding.domain.model.OnboardingScope;
import com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionFilter;
import com.buildingos.backoffice.onboarding.domain.model.OnboardingSessionStatus;
import com.buildingos.backoffice.onboarding.domain.repository.AssistedOnboardingSessionRepository;
import com.buildingos.backoffice.shared.application.Actor;
import com.buildingos.backoffice.shared.application.BusinessRuleException;
import com.buildingos.backoffice.shared.application.DependencyUnavailableException;
import com.buildingos.backoffice.shared.application.NotPermittedException;
import com.buildingos.backoffice.shared.application.port.out.UnitOfWork;
import com.buildingos.backoffice.shared.domain.model.DomainRuleException;
import com.buildingos.backoffice.shared.domain.model.EntityType;
import com.buildingos.backoffice.shared.domain.model.LifecycleTransition;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class OnboardingUseCaseTest {
    private static final Instant NOW = Instant.parse("2026-09-25T10:00:00Z");

    private final Map<UUID, AssistedOnboardingSession> rows = new LinkedHashMap<>();
    private final List<LifecycleTransition> audit = new ArrayList<>();
    private final Map<UUID, Set<String>> users = new LinkedHashMap<>();
    private final Set<UUID> buildings = new java.util.HashSet<>();
    private boolean authDown;
    private boolean buildingDown;
    private Instant clockNow = NOW;

    private final Actor operator = new Actor(UUID.randomUUID(), Set.of("PLATFORM_ADMIN"));
    private final Actor agent = new Actor(UUID.randomUUID(), Set.of("ONBOARDING_AGENT"));
    private final Actor otherAgent = new Actor(UUID.randomUUID(), Set.of("ONBOARDING_AGENT"));
    private final Actor stranger = new Actor(UUID.randomUUID(), Set.of());
    private final UUID building = UUID.randomUUID();

    private OnboardingSessionExpiry expiry;
    private StartOnboardingSessionService start;
    private OnboardingSessionChanges changes;

    @BeforeEach
    void wire() {
        users.put(agent.userId(), Set.of("ONBOARDING_AGENT"));
        users.put(stranger.userId(), Set.of());
        buildings.add(building);
        var repo = new InMemorySessions();
        LifecycleTransitionRepository transitions = new LifecycleTransitionRepository() {
            @Override public void append(LifecycleTransition t) { audit.add(t); }
            @Override public List<LifecycleTransition> findFor(EntityType type, UUID id) {
                return audit.stream().filter(t -> t.entityId().equals(id)).toList();
            }
            @Override public List<LifecycleTransition> list(Instant since, Instant until, EntityType type,
                    UUID actorUserId, int limit) {
                return List.of();
            }
        };
        UnitOfWork uow = new UnitOfWork() {
            @Override public <T> T inTransaction(Supplier<T> work) { return work.get(); }
        };
        Clock clock = new Clock() {
            @Override public ZoneOffset getZone() { return ZoneOffset.UTC; }
            @Override public Clock withZone(java.time.ZoneId zone) { return this; }
            @Override public Instant instant() { return clockNow; }
        };
        PlatformUserDirectory directory = id -> {
            if (authDown) throw new DependencyUnavailableException("auth-service", null);
            return Optional.ofNullable(users.get(id));
        };
        BuildingDirectory buildingDirectory = id -> {
            if (buildingDown) throw new DependencyUnavailableException("building-service", null);
            return buildings.contains(id);
        };
        expiry = new OnboardingSessionExpiry(repo, transitions, uow, clock);
        start = new StartOnboardingSessionService(repo, transitions, directory, buildingDirectory, expiry, uow,
                Duration.ofDays(30));
        changes = new OnboardingSessionChanges(repo, transitions, expiry, uow);
        this.repo = repo;
    }

    private InMemorySessions repo;

    private StartOnboardingSessionCommand command(UUID assignee) {
        return new StartOnboardingSessionCommand(building, assignee, List.of(OnboardingScope.ONBOARDING_VIEW_BUILDING),
                "Customer asked for help", null, NOW.plus(Duration.ofDays(3)));
    }

    private AssistedOnboardingSession created() {
        return start.execute(operator, command(agent.userId()));
    }

    @Test
    void operatorCreatesAssignedSessionAndAuditsIt() {
        var session = created();
        assertThat(session.status()).isEqualTo(OnboardingSessionStatus.ASSIGNED);
        assertThat(rows).containsKey(session.id());
        assertThat(audit).singleElement().satisfies(t -> {
            assertThat(t.fromStatus()).isNull();
            assertThat(t.toStatus()).isEqualTo("ASSIGNED");
            assertThat(t.actorUserId()).isEqualTo(operator.userId());
        });
    }

    @Test
    void onlyOperatorsCreate() {
        assertThatThrownBy(() -> start.execute(agent, command(agent.userId())))
                .isInstanceOf(NotPermittedException.class);
    }

    @Test
    void assigneeMustBeAnExistingOnboardingAgent() {
        assertCode(() -> start.execute(operator, command(stranger.userId())), "ASSIGNEE_NOT_ONBOARDING_AGENT");
        assertCode(() -> start.execute(operator, command(UUID.randomUUID())), "ASSIGNEE_NOT_ONBOARDING_AGENT");
        assertThat(rows).isEmpty();
    }

    @Test
    void buildingMustExist() {
        buildings.clear();
        assertCode(() -> start.execute(operator, command(agent.userId())), "BUILDING_NOT_FOUND");
        assertThat(rows).isEmpty();
    }

    @Test
    void dependencyDownStoresNothing() {
        authDown = true;
        assertThatThrownBy(this::created).isInstanceOf(DependencyUnavailableException.class);
        authDown = false;
        buildingDown = true;
        assertThatThrownBy(this::created).isInstanceOf(DependencyUnavailableException.class);
        assertThat(rows).isEmpty();
        assertThat(audit).isEmpty();
    }

    @Test
    void agentOnlyTransitionRejectsOperatorsAndHidesFromOtherAgents() {
        var session = created();
        var startWork = new StartOnboardingWorkService(changes);
        var cmd = new OnboardingTransitionCommand(session.id(), null);
        assertThatThrownBy(() -> startWork.execute(operator, cmd)).isInstanceOf(NotPermittedException.class);
        assertCode(() -> startWork.execute(otherAgent, cmd), "ONBOARDING_SESSION_NOT_FOUND");
        assertThatThrownBy(() -> startWork.execute(stranger, cmd)).isInstanceOf(NotPermittedException.class);
        assertThat(startWork.execute(agent, cmd).status()).isEqualTo(OnboardingSessionStatus.IN_PROGRESS);
        assertThat(last(audit).actorUserId()).isEqualTo(agent.userId());
    }

    @Test
    void cancelIsOperatorOnlyAndCompleteAllowsAgentOrOperator() {
        var cancel = new CancelOnboardingSessionService(changes);
        var complete = new CompleteOnboardingSessionService(changes);
        var first = created();
        assertThatThrownBy(() -> cancel.execute(agent, new OnboardingTransitionCommand(first.id(), null)))
                .isInstanceOf(NotPermittedException.class);
        assertThat(cancel.execute(operator, new OnboardingTransitionCommand(first.id(), "dup")).status())
                .isEqualTo(OnboardingSessionStatus.CANCELLED);
        assertThat(last(audit).reason()).isEqualTo("dup");
        var second = created();
        assertThat(complete.execute(agent, new OnboardingTransitionCommand(second.id(), null)).status())
                .isEqualTo(OnboardingSessionStatus.COMPLETED);
        var third = created();
        assertThat(complete.execute(operator, new OnboardingTransitionCommand(third.id(), null)).status())
                .isEqualTo(OnboardingSessionStatus.COMPLETED);
    }

    @Test
    void agentWhoLostTheRoleCannotAct() {
        var session = created();
        var demoted = new Actor(agent.userId(), Set.of());
        var complete = new CompleteOnboardingSessionService(changes);
        assertThatThrownBy(() -> complete.execute(demoted, new OnboardingTransitionCommand(session.id(), null)))
                .isInstanceOf(NotPermittedException.class);
    }

    @Test
    void readsPersistExpiryAsSystemTransitionBeforeAnswering() {
        var session = created();
        clockNow = session.expiresAt().plusSeconds(1);
        var get = new GetOnboardingSessionService(repo, expiry);
        assertThat(get.execute(agent, new GetOnboardingSessionQuery(session.id())).status())
                .isEqualTo(OnboardingSessionStatus.EXPIRED);
        assertThat(last(audit).toStatus()).isEqualTo("EXPIRED");
        assertThat(last(audit).actorUserId()).isNull();
        var complete = new CompleteOnboardingSessionService(changes);
        assertThatThrownBy(() -> complete.execute(operator, new OnboardingTransitionCommand(session.id(), null)))
                .isInstanceOfSatisfying(DomainRuleException.class, e -> {
                    assertThat(e.code()).isEqualTo("SESSION_ENDED");
                    assertThat(e.kind()).isEqualTo(DomainRuleException.Kind.CONFLICT);
                });
        assertThat(audit).hasSize(2);
    }

    @Test
    void listExpiresDueSessionsAndScopesAgentsToTheirOwn() {
        var mine = created();
        users.put(otherAgent.userId(), Set.of("ONBOARDING_AGENT"));
        start.execute(operator, command(otherAgent.userId()));
        var list = new ListOnboardingSessionsService(repo, expiry);
        assertThat(list.execute(operator, new ListOnboardingSessionsQuery(null, null, null, 0, 20)).total())
                .isEqualTo(2);
        var own = list.execute(agent, new ListOnboardingSessionsQuery(null, null, null, 0, 20));
        assertThat(own.items()).extracting(AssistedOnboardingSession::id).containsExactly(mine.id());
        assertThat(list.execute(agent, new ListOnboardingSessionsQuery(null, null, otherAgent.userId(), 0, 20))
                .items()).isEmpty();
        assertThatThrownBy(() -> list.execute(stranger, new ListOnboardingSessionsQuery(null, null, null, 0, 20)))
                .isInstanceOf(NotPermittedException.class);
        clockNow = NOW.plus(Duration.ofDays(4));
        var expired = list.execute(operator,
                new ListOnboardingSessionsQuery(OnboardingSessionStatus.EXPIRED, null, null, 0, 20));
        assertThat(expired.total()).isEqualTo(2);
    }

    private static <T> T last(List<T> list) { return list.get(list.size() - 1); }

    private static void assertCode(Runnable action, String code) {
        assertThatThrownBy(action::run).isInstanceOfSatisfying(BusinessRuleException.class,
                e -> assertThat(e.code()).isEqualTo(code));
    }

    private final class InMemorySessions implements AssistedOnboardingSessionRepository {
        @Override public void insert(AssistedOnboardingSession s) { rows.put(s.id(), s); }
        @Override public void update(AssistedOnboardingSession s) { rows.put(s.id(), s); }
        @Override public Optional<AssistedOnboardingSession> findById(UUID id) {
            return Optional.ofNullable(rows.get(id));
        }
        @Override public Optional<AssistedOnboardingSession> findByIdForUpdate(UUID id) { return findById(id); }
        @Override public List<AssistedOnboardingSession> lockDueForExpiry(Instant now, int limit) {
            return rows.values().stream().filter(s -> s.isDue(now)).limit(limit).toList();
        }
        @Override public List<AssistedOnboardingSession> search(OnboardingSessionFilter f, int offset, int limit) {
            return matching(f).stream().sorted(Comparator.comparing(AssistedOnboardingSession::startedAt).reversed())
                    .skip(offset).limit(limit).toList();
        }
        @Override public long count(OnboardingSessionFilter f) { return matching(f).size(); }

        private List<AssistedOnboardingSession> matching(OnboardingSessionFilter f) {
            return rows.values().stream()
                    .filter(s -> f.status() == null || s.status() == f.status())
                    .filter(s -> f.buildingId() == null || s.buildingId().equals(f.buildingId()))
                    .filter(s -> f.agentUserId() == null || s.assignedAgentUserId().equals(f.agentUserId()))
                    .toList();
        }
    }
}
