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

@Configuration(proxyBeanMethods = false)
public class GatewayRouteConfig {
    @Bean
    RouterFunction<ServerResponse> routes(
            @Value("${ACCOUNT_SERVICE_URL}") String accountUrl,
            @Value("${BUILDING_SERVICE_URL}") String buildingUrl) {
        return metadataRoute("account", accountUrl)
                .and(metadataRoute("building", buildingUrl))
                .and(otpRoute("start", accountUrl))
                .and(otpRoute("verify", accountUrl));
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

    private RouterFunction<ServerResponse> otpRoute(String step, String accountTarget) {
        String path = "/api/v1/auth/otp/" + step;
        return route("otp-" + step)
                .POST(path, http())
                .before(uri(accountTarget))
                .before(request -> setRequestHeader(CorrelationFilter.HEADER,
                        CorrelationFilter.traceId(request.servletRequest())).apply(request))
                .build();
    }
}
