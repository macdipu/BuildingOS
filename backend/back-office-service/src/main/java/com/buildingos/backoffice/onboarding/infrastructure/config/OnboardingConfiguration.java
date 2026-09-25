package com.buildingos.backoffice.onboarding.infrastructure.config;

import com.buildingos.backoffice.onboarding.application.OnboardingSessionChanges;
import com.buildingos.backoffice.onboarding.application.OnboardingSessionExpiry;
import com.buildingos.backoffice.onboarding.application.awaitonboardingcustomer.AwaitOnboardingCustomerService;
import com.buildingos.backoffice.onboarding.application.awaitonboardingcustomer.AwaitOnboardingCustomerUseCase;
import com.buildingos.backoffice.onboarding.application.cancelonboardingsession.CancelOnboardingSessionService;
import com.buildingos.backoffice.onboarding.application.cancelonboardingsession.CancelOnboardingSessionUseCase;
import com.buildingos.backoffice.onboarding.application.completeonboardingsession.CompleteOnboardingSessionService;
import com.buildingos.backoffice.onboarding.application.completeonboardingsession.CompleteOnboardingSessionUseCase;
import com.buildingos.backoffice.onboarding.application.getonboardingsession.GetOnboardingSessionService;
import com.buildingos.backoffice.onboarding.application.getonboardingsession.GetOnboardingSessionUseCase;
import com.buildingos.backoffice.onboarding.application.listonboardingsessions.ListOnboardingSessionsService;
import com.buildingos.backoffice.onboarding.application.listonboardingsessions.ListOnboardingSessionsUseCase;
import com.buildingos.backoffice.onboarding.application.port.out.BuildingDirectory;
import com.buildingos.backoffice.onboarding.application.port.out.PlatformUserDirectory;
import com.buildingos.backoffice.onboarding.application.startonboardingsession.StartOnboardingSessionService;
import com.buildingos.backoffice.onboarding.application.startonboardingsession.StartOnboardingSessionUseCase;
import com.buildingos.backoffice.onboarding.application.startonboardingwork.StartOnboardingWorkService;
import com.buildingos.backoffice.onboarding.application.startonboardingwork.StartOnboardingWorkUseCase;
import com.buildingos.backoffice.onboarding.domain.repository.AssistedOnboardingSessionRepository;
import com.buildingos.backoffice.shared.application.port.out.UnitOfWork;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(OnboardingProperties.class)
public class OnboardingConfiguration {
    @Bean
    OnboardingSessionExpiry onboardingSessionExpiry(AssistedOnboardingSessionRepository sessions,
            LifecycleTransitionRepository transitions, UnitOfWork uow, Clock clock) {
        return new OnboardingSessionExpiry(sessions, transitions, uow, clock);
    }

    @Bean
    OnboardingSessionChanges onboardingSessionChanges(AssistedOnboardingSessionRepository sessions,
            LifecycleTransitionRepository transitions, OnboardingSessionExpiry expiry, UnitOfWork uow) {
        return new OnboardingSessionChanges(sessions, transitions, expiry, uow);
    }

    @Bean
    StartOnboardingSessionUseCase startOnboardingSession(AssistedOnboardingSessionRepository sessions,
            LifecycleTransitionRepository transitions, PlatformUserDirectory users, BuildingDirectory buildings,
            OnboardingSessionExpiry expiry, UnitOfWork uow, OnboardingProperties properties) {
        return new StartOnboardingSessionService(sessions, transitions, users, buildings, expiry, uow,
                properties.maxDuration());
    }

    @Bean
    ListOnboardingSessionsUseCase listOnboardingSessions(AssistedOnboardingSessionRepository sessions,
            OnboardingSessionExpiry expiry) {
        return new ListOnboardingSessionsService(sessions, expiry);
    }

    @Bean
    GetOnboardingSessionUseCase getOnboardingSession(AssistedOnboardingSessionRepository sessions,
            OnboardingSessionExpiry expiry) {
        return new GetOnboardingSessionService(sessions, expiry);
    }

    @Bean
    StartOnboardingWorkUseCase startOnboardingWork(OnboardingSessionChanges changes) {
        return new StartOnboardingWorkService(changes);
    }

    @Bean
    AwaitOnboardingCustomerUseCase awaitOnboardingCustomer(OnboardingSessionChanges changes) {
        return new AwaitOnboardingCustomerService(changes);
    }

    @Bean
    CompleteOnboardingSessionUseCase completeOnboardingSession(OnboardingSessionChanges changes) {
        return new CompleteOnboardingSessionService(changes);
    }

    @Bean
    CancelOnboardingSessionUseCase cancelOnboardingSession(OnboardingSessionChanges changes) {
        return new CancelOnboardingSessionService(changes);
    }
}
