package com.buildingos.building.duplicate.infrastructure.config;

import com.buildingos.building.buildingapplication.application.ApplicationChanges;
import com.buildingos.building.duplicate.application.finddupsignals.FindDuplicateSignalsService;
import com.buildingos.building.duplicate.application.finddupsignals.FindDuplicateSignalsUseCase;
import com.buildingos.building.duplicate.domain.model.DuplicateMatcher;
import com.buildingos.building.duplicate.domain.repository.DuplicateCandidateRepository;
import java.util.Set;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DuplicateProperties.class)
public class DuplicateConfiguration {
    @Bean
    DuplicateMatcher duplicateMatcher(DuplicateProperties properties) {
        return new DuplicateMatcher(Set.copyOf(properties.ignoredTokens()), properties.radiusMeters());
    }

    @Bean
    FindDuplicateSignalsUseCase findDuplicateSignals(ApplicationChanges applications,
            DuplicateCandidateRepository candidates, DuplicateMatcher matcher) {
        return new FindDuplicateSignalsService(applications, candidates, matcher);
    }
}
