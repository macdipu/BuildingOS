package com.buildingos.subscription.plan.presentation.rest.request;

import java.util.List;
import java.util.Map;

/** Create/edit body. {@code code} is used on create only; it is immutable afterwards. */
public record PlanRequest(String code, String name, List<String> billingCycles, Boolean selfService,
        Map<String, Object> entitlements) {}
