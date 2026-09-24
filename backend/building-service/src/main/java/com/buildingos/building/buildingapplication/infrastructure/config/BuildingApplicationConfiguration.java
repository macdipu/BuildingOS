package com.buildingos.building.buildingapplication.infrastructure.config;

import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.building.domain.repository.BuildingRepository;
import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.buildingapplication.application.approveapplication.ApproveApplicationService;
import com.buildingos.building.buildingapplication.application.approveapplication.ApproveApplicationUseCase;
import com.buildingos.building.buildingapplication.application.port.out.CreationFeeGateway;
import com.buildingos.building.buildingapplication.application.port.out.UserProvisioning;
import com.buildingos.building.buildingapplication.application.createapplication.CreateApplicationService;
import com.buildingos.building.buildingapplication.application.createapplication.CreateApplicationUseCase;
import com.buildingos.building.buildingapplication.application.getapplication.GetApplicationService;
import com.buildingos.building.buildingapplication.application.getapplication.GetApplicationUseCase;
import com.buildingos.building.buildingapplication.application.getapplicationhistory.GetApplicationHistoryService;
import com.buildingos.building.buildingapplication.application.getapplicationhistory.GetApplicationHistoryUseCase;
import com.buildingos.building.buildingapplication.application.listapplications.ListApplicationsService;
import com.buildingos.building.buildingapplication.application.listapplications.ListApplicationsUseCase;
import com.buildingos.building.buildingapplication.application.listmyapplications.ListMyApplicationsService;
import com.buildingos.building.buildingapplication.application.listmyapplications.ListMyApplicationsUseCase;
import com.buildingos.building.buildingapplication.application.rejectapplication.RejectApplicationService;
import com.buildingos.building.buildingapplication.application.rejectapplication.RejectApplicationUseCase;
import com.buildingos.building.buildingapplication.application.requestinformation.RequestInformationService;
import com.buildingos.building.buildingapplication.application.requestinformation.RequestInformationUseCase;
import com.buildingos.building.buildingapplication.application.startreview.StartReviewService;
import com.buildingos.building.buildingapplication.application.startreview.StartReviewUseCase;
import com.buildingos.building.buildingapplication.application.submitapplication.SubmitApplicationService;
import com.buildingos.building.buildingapplication.application.submitapplication.SubmitApplicationUseCase;
import com.buildingos.building.buildingapplication.application.updateapplication.UpdateApplicationService;
import com.buildingos.building.buildingapplication.application.updateapplication.UpdateApplicationUseCase;
import com.buildingos.building.buildingapplication.domain.repository.BuildingApplicationRepository;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.repository.LifecycleTransitionRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class BuildingApplicationConfiguration {
    @Bean
    ApplicationChanges applicationChanges(BuildingApplicationRepository applications,
            LifecycleTransitionRepository transitions, UnitOfWork uow, Clock clock) {
        return new ApplicationChanges(applications, transitions, uow, clock);
    }

    @Bean
    CreateApplicationUseCase createApplication(BuildingApplicationRepository applications,
            LifecycleTransitionRepository transitions, UnitOfWork uow, Clock clock) {
        return new CreateApplicationService(applications, transitions, uow, clock);
    }

    @Bean
    UpdateApplicationUseCase updateApplication(ApplicationChanges changes) { return new UpdateApplicationService(changes); }

    @Bean
    SubmitApplicationUseCase submitApplication(ApplicationChanges changes) { return new SubmitApplicationService(changes); }

    @Bean
    GetApplicationUseCase getApplication(ApplicationChanges changes) { return new GetApplicationService(changes); }

    @Bean
    ListMyApplicationsUseCase listMyApplications(BuildingApplicationRepository applications) {
        return new ListMyApplicationsService(applications);
    }

    @Bean
    ListApplicationsUseCase listApplications(BuildingApplicationRepository applications) {
        return new ListApplicationsService(applications);
    }

    @Bean
    StartReviewUseCase startReview(ApplicationChanges changes) { return new StartReviewService(changes); }

    @Bean
    RequestInformationUseCase requestInformation(ApplicationChanges changes) {
        return new RequestInformationService(changes);
    }

    @Bean
    RejectApplicationUseCase rejectApplication(ApplicationChanges changes) { return new RejectApplicationService(changes); }

    @Bean
    GetApplicationHistoryUseCase getApplicationHistory(ApplicationChanges changes,
            LifecycleTransitionRepository transitions) {
        return new GetApplicationHistoryService(changes, transitions);
    }

    @Bean
    ApproveApplicationUseCase approveApplication(BuildingApplicationRepository applications,
            BuildingRepository buildings, BuildingMembershipRepository memberships,
            LifecycleTransitionRepository transitions, CreationFeeGateway fees, UserProvisioning users,
            UnitOfWork uow, Clock clock) {
        return new ApproveApplicationService(applications, buildings, memberships, transitions, fees, users, uow,
                clock);
    }
}
