package com.buildingos.gateway.routing.config;

import com.buildingos.platform.web.correlation.CorrelationFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.function.RouterFunction;
import org.springframework.web.servlet.function.ServerResponse;
import static org.springframework.cloud.gateway.server.mvc.handler.GatewayRouterFunctions.route;
import static org.springframework.cloud.gateway.server.mvc.handler.HandlerFunctions.http;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.uri;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.setPath;
import static org.springframework.cloud.gateway.server.mvc.filter.BeforeFilterFunctions.setRequestHeader;
import static org.springframework.web.servlet.function.RequestPredicates.path;

@Configuration(proxyBeanMethods = false)
public class GatewayRouteConfig {
    @Bean
    RouterFunction<ServerResponse> routes(
            @Value("${AUTH_SERVICE_URL}") String authUrl,
            @Value("${BUILDING_SERVICE_URL}") String buildingUrl,
            @Value("${SUBSCRIPTION_SERVICE_URL}") String subscriptionUrl) {
        return metadataRoute("auth", authUrl)
                .and(metadataRoute("building", buildingUrl))
                .and(metadataRoute("subscription", subscriptionUrl))
                .and(passThroughRoute("subscription-api", subscriptionUrl,
                        "/api/v1/platform/subscription-plans", "/api/v1/platform/subscription-plans/**",
                        "/api/v1/platform/free-tier", "/api/v1/platform/users/*/subscription",
                        "/api/v1/platform/fees/**", "/api/v1/me/plans", "/api/v1/me/subscription",
                        "/api/v1/me/entitlements"))
                .and(passThroughRoute("building-api", buildingUrl,
                        "/api/v1/building-applications", "/api/v1/building-applications/**",
                        "/api/v1/me/building-applications", "/api/v1/platform/building-applications",
                        "/api/v1/platform/building-applications/**", "/api/v1/platform/buildings/**"))
                .and(otpRoute("start", authUrl))
                .and(otpRoute("verify", authUrl));
    }

    private RouterFunction<ServerResponse> metadataRoute(String service, String target) {
        return route(service)
                .GET("/api/v1/platform/" + service, http())
                .before(uri(target))
                .before(setPath("/internal/platform/info"))
                .before(request -> setRequestHeader(CorrelationFilter.HEADER,
                        CorrelationFilter.traceId(request.servletRequest())).apply(request))
                .build();
    }

    /** Forwards the listed paths unchanged (all methods); the downstream re-validates the bearer token. */
    private RouterFunction<ServerResponse> passThroughRoute(String id, String target, String... patterns) {
        var predicate = path(patterns[0]);
        for (int i = 1; i < patterns.length; i++) {
            predicate = predicate.or(path(patterns[i]));
        }
        return route(id)
                .route(predicate, http())
                .before(uri(target))
                .before(request -> setRequestHeader(CorrelationFilter.HEADER,
                        CorrelationFilter.traceId(request.servletRequest())).apply(request))
                .build();
    }

    private RouterFunction<ServerResponse> otpRoute(String step, String authTarget) {
        String path = "/api/v1/auth/otp/" + step;
        return route("otp-" + step)
                .POST(path, http())
                .before(uri(authTarget))
                .before(request -> setRequestHeader(CorrelationFilter.HEADER,
                        CorrelationFilter.traceId(request.servletRequest())).apply(request))
                .build();
    }
}
