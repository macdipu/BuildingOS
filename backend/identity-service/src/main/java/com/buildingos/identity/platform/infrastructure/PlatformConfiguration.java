package com.buildingos.identity.platform.infrastructure;

import com.buildingos.identity.platform.application.port.in.GetServiceMetadata;
import com.buildingos.identity.platform.application.usecase.GetServiceMetadataService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class PlatformConfiguration {
    @Bean
    GetServiceMetadata getServiceMetadata() { return new GetServiceMetadataService(); }
}
