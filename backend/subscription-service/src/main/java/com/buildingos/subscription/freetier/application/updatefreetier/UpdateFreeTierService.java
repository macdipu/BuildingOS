package com.buildingos.subscription.freetier.application.updatefreetier;

import com.buildingos.subscription.catalog.domain.model.Entitlements;
import com.buildingos.subscription.freetier.domain.repository.FreeTierRepository;
import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.shared.application.port.out.AuditRecorder;
import com.buildingos.subscription.shared.application.port.out.UnitOfWork;
import java.time.Clock;

public final class UpdateFreeTierService implements UpdateFreeTierUseCase {
    private final FreeTierRepository freeTier;
    private final AuditRecorder audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public UpdateFreeTierService(FreeTierRepository freeTier, AuditRecorder audit, UnitOfWork unitOfWork, Clock clock) {
        this.freeTier = freeTier;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public Entitlements execute(Actor actor, UpdateFreeTierCommand command) {
        actor.requireRevenueAdmin();
        return unitOfWork.inTransaction(() -> {
            var before = freeTier.get();
            freeTier.save(command.entitlements());
            audit.record(new AuditRecorder.Entry(actor.userId(), "FREE_TIER_UPDATED", "FREE_TIER", "1",
                    before.toKeys(), command.entitlements().toKeys(), clock.instant()));
            return command.entitlements();
        });
    }
}
