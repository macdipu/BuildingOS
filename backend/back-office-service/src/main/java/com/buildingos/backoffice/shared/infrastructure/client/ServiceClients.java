package com.buildingos.backoffice.shared.infrastructure.client;

import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({ServiceClientProperties.class, ServiceCredentialProperties.class})
public class ServiceClients {
    @Bean
    RestClient authServiceClient(ServiceClientProperties properties) {
        return client(properties, properties.authUrl());
    }

    @Bean
    RestClient buildingServiceClient(ServiceClientProperties properties) {
        return client(properties, properties.buildingUrl());
    }

    /** Directory lookups (D-38) authenticate as this service, not as the caller. */
    @Bean
    ServiceTokenProvider serviceTokenProvider(ServiceClientProperties properties,
            ServiceCredentialProperties credential) {
        return new ServiceTokenProvider(client(properties, properties.authUrl(), null), credential, Clock.systemUTC());
    }

    @Bean
    RestClient authDirectoryClient(ServiceClientProperties properties, ServiceTokenProvider tokens) {
        return client(properties, properties.authUrl(), new ServiceTokenAuthorization(tokens));
    }

    @Bean
    RestClient buildingDirectoryClient(ServiceClientProperties properties, ServiceTokenProvider tokens) {
        return client(properties, properties.buildingUrl(), new ServiceTokenAuthorization(tokens));
    }

    private static RestClient client(ServiceClientProperties properties, String baseUrl) {
        return client(properties, baseUrl, new BearerTokenRelay());
    }

    private static RestClient client(ServiceClientProperties properties, String baseUrl,
            ClientHttpRequestInterceptor authorization) {
        var factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        var builder = RestClient.builder().baseUrl(baseUrl).requestFactory(factory);
        return (authorization == null ? builder : builder.requestInterceptor(authorization)).build();
    }
}
