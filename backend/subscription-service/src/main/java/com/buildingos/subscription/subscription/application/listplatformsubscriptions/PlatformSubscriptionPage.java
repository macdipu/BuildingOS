package com.buildingos.subscription.subscription.application.listplatformsubscriptions;

import com.buildingos.subscription.subscription.domain.model.PlatformSubscriptionRow;
import java.util.List;

public record PlatformSubscriptionPage(List<PlatformSubscriptionRow> items, int page, int size, long total) {
    public static final int MAX_SIZE = 100;

    public PlatformSubscriptionPage {
        items = List.copyOf(items);
    }
}
