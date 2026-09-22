package com.buildingos.building.platform.infrastructure;

import com.buildingos.building.platform.application.port.in.GetServiceMetadata;
import com.buildingos.building.platform.application.usecase.GetServiceMetadataService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PlatformConfiguration {
    @Bean
    GetServiceMetadata getServiceMetadata() { return new GetServiceMetadataService(); }
}
