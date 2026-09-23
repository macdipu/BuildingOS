package com.buildingos.platform.web.config;

import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.exception.PlatformErrorHandler;
import com.buildingos.platform.web.response.ApiError;
import com.buildingos.platform.web.security.SecuritySettings;
import java.net.URI;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.Ordered;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.http.MediaType;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import tools.jackson.databind.ObjectMapper;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecuritySettings.class)
@Import(PlatformErrorHandler.class)
public class PlatformWebConfiguration {
    @Bean
    FilterRegistrationBean<CorrelationFilter> correlationFilter() {
        var filter = new FilterRegistrationBean<>(new CorrelationFilter());
        filter.setOrder(Ordered.HIGHEST_PRECEDENCE);
        return filter;
    }

    @Bean
    JwtDecoder jwtDecoder(SecuritySettings settings, Environment environment) {
        boolean local = environment.acceptsProfiles(Profiles.of("local", "test"));
        for (String value : List.of(settings.issuer(), settings.jwkSetUri())) {
            URI uri = URI.create(value);
            if (uri.getHost() == null || uri.getUserInfo() != null
                    || (!"https".equals(uri.getScheme()) && !(local && "http".equals(uri.getScheme())))) {
                throw new IllegalArgumentException("JWT issuer and JWKS must use HTTPS outside local/test profiles");
            }
        }
        var decoder = NimbusJwtDecoder.withJwkSetUri(settings.jwkSetUri())
                .jwsAlgorithm(SignatureAlgorithm.RS256).build();
        OAuth2TokenValidator<Jwt> audience = jwt -> jwt.getAudience().contains(settings.audience())
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Invalid audience", null));
        decoder.setJwtValidator(new DelegatingOAuth2TokenValidator<>(
                JwtValidators.createDefaultWithIssuer(settings.issuer()), audience));
        return decoder;
    }

    @Bean
    SecurityFilterChain security(HttpSecurity http, ObjectMapper mapper, SecuritySettings settings) throws Exception {
        http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> {
                auth.requestMatchers("/actuator/health", "/actuator/health/**").permitAll();
                if (!settings.publicPaths().isEmpty()) {
                    auth.requestMatchers(settings.publicPaths().toArray(String[]::new)).permitAll();
                }
                auth.requestMatchers("/actuator/**").hasAuthority("SCOPE_platform.observe");
                auth.anyRequest().authenticated();
            })
            .oauth2ResourceServer(oauth -> oauth.jwt(Customizer.withDefaults())
                .authenticationEntryPoint((request, response, error) -> {
                    response.setStatus(401);
                    response.setHeader("WWW-Authenticate", "Bearer");
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    mapper.writeValue(response.getOutputStream(), ApiError.of("AUTH_REQUIRED",
                            "Authentication is required", CorrelationFilter.traceId(request)));
                }))
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, error) -> {
                    response.setStatus(401);
                    response.setHeader("WWW-Authenticate", "Bearer");
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    mapper.writeValue(response.getOutputStream(), ApiError.of("AUTH_REQUIRED",
                            "Authentication is required", CorrelationFilter.traceId(request)));
                })
                .accessDeniedHandler((request, response, error) -> {
                    response.setStatus(403);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    mapper.writeValue(response.getOutputStream(), ApiError.of("ACCESS_DENIED",
                            "Access is denied", CorrelationFilter.traceId(request)));
                }));
        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(SecuritySettings settings) {
        var configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(settings.allowedOrigins());
        configuration.setAllowedMethods(List.of("GET", "POST", "OPTIONS"));
        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", CorrelationFilter.HEADER));
        configuration.setExposedHeaders(List.of(CorrelationFilter.HEADER));
        configuration.setAllowCredentials(false);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
