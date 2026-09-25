package com.buildingos.backoffice.supportsession.infrastructure.config;

import com.buildingos.backoffice.shared.application.port.out.BuildingDirectory;
import com.buildingos.backoffice.shared.application.port.out.PlatformUserDirectory;
import com.buildingos.backoffice.shared.application.port.out.UnitOfWork;
import com.buildingos.backoffice.shared.domain.repository.LifecycleTransitionRepository;
import com.buildingos.backoffice.supportsession.application.ElevatedApprovalDecisions;
import com.buildingos.backoffice.supportsession.application.SupportSessionExpiry;
import com.buildingos.backoffice.supportsession.application.approveelevatedapproval.ApproveElevatedApprovalService;
import com.buildingos.backoffice.supportsession.application.approveelevatedapproval.ApproveElevatedApprovalUseCase;
import com.buildingos.backoffice.supportsession.application.checksupportscope.CheckSupportScopeService;
import com.buildingos.backoffice.supportsession.application.checksupportscope.CheckSupportScopeUseCase;
import com.buildingos.backoffice.supportsession.application.denyelevatedapproval.DenyElevatedApprovalService;
import com.buildingos.backoffice.supportsession.application.denyelevatedapproval.DenyElevatedApprovalUseCase;
import com.buildingos.backoffice.supportsession.application.endsupportsession.EndSupportSessionService;
import com.buildingos.backoffice.supportsession.application.endsupportsession.EndSupportSessionUseCase;
import com.buildingos.backoffice.supportsession.application.getsupportsession.GetSupportSessionService;
import com.buildingos.backoffice.supportsession.application.getsupportsession.GetSupportSessionUseCase;
import com.buildingos.backoffice.supportsession.application.listelevatedapprovals.ListElevatedApprovalsService;
import com.buildingos.backoffice.supportsession.application.listelevatedapprovals.ListElevatedApprovalsUseCase;
import com.buildingos.backoffice.supportsession.application.listsupportsessions.ListSupportSessionsService;
import com.buildingos.backoffice.supportsession.application.listsupportsessions.ListSupportSessionsUseCase;
import com.buildingos.backoffice.supportsession.application.requestelevatedapproval.RequestElevatedApprovalService;
import com.buildingos.backoffice.supportsession.application.requestelevatedapproval.RequestElevatedApprovalUseCase;
import com.buildingos.backoffice.supportsession.application.startsupportsession.StartSupportSessionService;
import com.buildingos.backoffice.supportsession.application.startsupportsession.StartSupportSessionUseCase;
import com.buildingos.backoffice.supportsession.domain.repository.ElevatedApprovalRequestRepository;
import com.buildingos.backoffice.supportsession.domain.repository.SupportSessionRepository;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SupportProperties.class)
public class SupportConfiguration {
    @Bean
    SupportSessionExpiry supportSessionExpiry(SupportSessionRepository sessions,
            LifecycleTransitionRepository transitions, UnitOfWork uow, Clock clock) {
        return new SupportSessionExpiry(sessions, transitions, uow, clock);
    }

    @Bean
    ElevatedApprovalDecisions elevatedApprovalDecisions(SupportSessionRepository sessions,
            ElevatedApprovalRequestRepository approvals, LifecycleTransitionRepository transitions,
            SupportSessionExpiry expiry, UnitOfWork uow) {
        return new ElevatedApprovalDecisions(sessions, approvals, transitions, expiry, uow);
    }

    @Bean
    StartSupportSessionUseCase startSupportSession(SupportSessionRepository sessions,
            ElevatedApprovalRequestRepository approvals, LifecycleTransitionRepository transitions,
            PlatformUserDirectory users, BuildingDirectory buildings, SupportSessionExpiry expiry, UnitOfWork uow,
            SupportProperties properties) {
        return new StartSupportSessionService(sessions, approvals, transitions, users, buildings, expiry, uow,
                properties.maxDuration());
    }

    @Bean
    ListSupportSessionsUseCase listSupportSessions(SupportSessionRepository sessions, SupportSessionExpiry expiry) {
        return new ListSupportSessionsService(sessions, expiry);
    }

    @Bean
    GetSupportSessionUseCase getSupportSession(SupportSessionRepository sessions,
            ElevatedApprovalRequestRepository approvals, SupportSessionExpiry expiry) {
        return new GetSupportSessionService(sessions, approvals, expiry);
    }

    @Bean
    EndSupportSessionUseCase endSupportSession(SupportSessionRepository sessions,
            ElevatedApprovalRequestRepository approvals, LifecycleTransitionRepository transitions,
            SupportSessionExpiry expiry, UnitOfWork uow) {
        return new EndSupportSessionService(sessions, approvals, transitions, expiry, uow);
    }

    @Bean
    RequestElevatedApprovalUseCase requestElevatedApproval(SupportSessionRepository sessions,
            ElevatedApprovalRequestRepository approvals, LifecycleTransitionRepository transitions,
            SupportSessionExpiry expiry, UnitOfWork uow) {
        return new RequestElevatedApprovalService(sessions, approvals, transitions, expiry, uow);
    }

    @Bean
    ApproveElevatedApprovalUseCase approveElevatedApproval(ElevatedApprovalDecisions decisions) {
        return new ApproveElevatedApprovalService(decisions);
    }

    @Bean
    DenyElevatedApprovalUseCase denyElevatedApproval(ElevatedApprovalDecisions decisions) {
        return new DenyElevatedApprovalService(decisions);
    }

    @Bean
    ListElevatedApprovalsUseCase listElevatedApprovals(ElevatedApprovalRequestRepository approvals,
            SupportSessionExpiry expiry) {
        return new ListElevatedApprovalsService(approvals, expiry);
    }

    @Bean
    CheckSupportScopeUseCase checkSupportScope(SupportSessionRepository sessions,
            ElevatedApprovalRequestRepository approvals, SupportSessionExpiry expiry) {
        return new CheckSupportScopeService(sessions, approvals, expiry);
    }
}
