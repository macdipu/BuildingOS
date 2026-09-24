package com.buildingos.building.shared.infrastructure.client;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ServiceClientProperties.class)
public class ServiceClients {
    @Bean
    RestClient authServiceClient(ServiceClientProperties properties) {
        return client(properties, properties.authUrl());
    }

    @Bean
    RestClient subscriptionServiceClient(ServiceClientProperties properties) {
        return client(properties, properties.subscriptionUrl());
    }

    private static RestClient client(ServiceClientProperties properties, String baseUrl) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return RestClient.builder().baseUrl(baseUrl).requestFactory(factory).requestInterceptor(new BearerTokenRelay())
                .build();
    }
}
