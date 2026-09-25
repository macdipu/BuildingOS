package com.buildingos.backoffice.servicemeta.infrastructure.config;

import com.buildingos.backoffice.servicemeta.application.getservicemetadata.GetServiceMetadataService;
import com.buildingos.backoffice.servicemeta.application.getservicemetadata.GetServiceMetadataUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ServiceMetadataConfiguration {
    @Bean
    GetServiceMetadataUseCase getServiceMetadata() { return new GetServiceMetadataService(); }
}
