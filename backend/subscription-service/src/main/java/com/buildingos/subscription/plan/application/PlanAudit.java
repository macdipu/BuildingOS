package com.buildingos.subscription.plan.application;

import com.buildingos.subscription.plan.domain.model.SubscriptionPlan;
import java.util.LinkedHashMap;
import java.util.Map;

/** Audit snapshot of a plan. */
public final class PlanAudit {
    private PlanAudit() {}

    public static Map<String, Object> snapshot(SubscriptionPlan plan) {
        var map = new LinkedHashMap<String, Object>();
        map.put("code", plan.code().value());
        map.put("name", plan.name());
        map.put("status", plan.status().name());
        map.put("billingCycles", plan.billingCycles().stream().map(Enum::name).sorted().toList());
        map.put("selfService", plan.selfService());
        map.put("entitlements", plan.entitlements().toKeys());
        return map;
    }
}
