package com.buildingos.subscription.freetier.application.getfreetier;

import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.freetier.domain.repository.FreeTierRepository;
import com.buildingos.subscription.shared.application.Actor;

public final class GetFreeTierService implements GetFreeTierUseCase {
    private final FreeTierRepository freeTier;

    public GetFreeTierService(FreeTierRepository freeTier) { this.freeTier = freeTier; }

    @Override
    public Entitlements execute(Actor actor) {
        actor.requireRevenueAdmin();
        return freeTier.get();
    }
}
