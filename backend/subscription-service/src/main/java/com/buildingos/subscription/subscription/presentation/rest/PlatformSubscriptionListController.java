package com.buildingos.subscription.subscription.presentation.rest;

import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.subscription.shared.presentation.rest.CurrentActor;
import com.buildingos.subscription.shared.presentation.rest.Enums;
import com.buildingos.subscription.subscription.application.listplatformsubscriptions.ListPlatformSubscriptionsQuery;
import com.buildingos.subscription.subscription.application.listplatformsubscriptions.ListPlatformSubscriptionsUseCase;
import com.buildingos.subscription.subscription.domain.model.SubscriptionStatus;
import com.buildingos.subscription.subscription.presentation.rest.response.PlatformSubscriptionResponse;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Back-office: platform-wide list of user subscriptions (D-35). */
@RestController
@RequestMapping("/api/v1/platform/subscriptions")
public class PlatformSubscriptionListController {
    private final ListPlatformSubscriptionsUseCase list;

    public PlatformSubscriptionListController(ListPlatformSubscriptionsUseCase list) { this.list = list; }

    @GetMapping
    public ApiEnvelope<List<PlatformSubscriptionResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String status, @RequestParam(required = false) String plan,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        SubscriptionStatus parsed = status == null || status.isBlank() ? null
                : Enums.parse(SubscriptionStatus.class, status, "status");
        var result = list.execute(CurrentActor.from(jwt), new ListPlatformSubscriptionsQuery(parsed, plan, page, size));
        return new ApiEnvelope<>(true, result.items().stream().map(PlatformSubscriptionResponse::of).toList(),
                Map.of("page", result.page(), "size", result.size(), "total", result.total()),
                CorrelationFilter.traceId(request));
    }
}
