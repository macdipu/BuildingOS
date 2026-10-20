package com.buildingos.subscription.subscription.application.listplatformsubscriptions;

import com.buildingos.subscription.subscription.domain.model.SubscriptionStatus;

/** {@code status} null = every status; {@code planCode} null = every plan. */
public record ListPlatformSubscriptionsQuery(SubscriptionStatus status, String planCode, int page, int size) {}
