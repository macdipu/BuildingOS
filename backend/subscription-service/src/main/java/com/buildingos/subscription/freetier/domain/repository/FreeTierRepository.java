package com.buildingos.subscription.freetier.domain.repository;

import com.buildingos.subscription.catalog.domain.model.Entitlements;

/** Single platform record of what every user gets without a subscription (D-23). */
public interface FreeTierRepository {
    Entitlements get();
    void save(Entitlements entitlements);
}
