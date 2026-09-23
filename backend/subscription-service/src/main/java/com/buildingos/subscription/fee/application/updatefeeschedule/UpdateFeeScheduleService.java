package com.buildingos.subscription.fee.application.updatefeeschedule;

import com.buildingos.subscription.fee.domain.model.FeeSchedule;
import com.buildingos.subscription.fee.domain.repository.FeeScheduleRepository;
import com.buildingos.subscription.shared.application.Actor;
import com.buildingos.subscription.shared.application.port.out.AuditRecorder;
import com.buildingos.subscription.shared.application.port.out.UnitOfWork;
import java.time.Clock;
import java.util.LinkedHashMap;
import java.util.Map;

public final class UpdateFeeScheduleService implements UpdateFeeScheduleUseCase {
    private final FeeScheduleRepository schedules;
    private final AuditRecorder audit;
    private final UnitOfWork unitOfWork;
    private final Clock clock;

    public UpdateFeeScheduleService(FeeScheduleRepository schedules, AuditRecorder audit, UnitOfWork unitOfWork,
            Clock clock) {
        this.schedules = schedules;
        this.audit = audit;
        this.unitOfWork = unitOfWork;
        this.clock = clock;
    }

    @Override
    public FeeSchedule execute(Actor actor, UpdateFeeScheduleCommand command) {
        actor.requireRevenueAdmin();
        return unitOfWork.inTransaction(() -> {
            var now = clock.instant();
            var before = schedules.find(command.code()).map(UpdateFeeScheduleService::snapshot).orElse(null);
            var schedule = new FeeSchedule(command.code(), command.price(), command.required(), now);
            schedules.save(schedule);
            audit.record(new AuditRecorder.Entry(actor.userId(), "FEE_SCHEDULE_UPDATED", "FEE_SCHEDULE",
                    command.code().name(), before, snapshot(schedule), now));
            return schedule;
        });
    }

    private static Map<String, Object> snapshot(FeeSchedule schedule) {
        var map = new LinkedHashMap<String, Object>();
        map.put("amount", schedule.price().amount().toPlainString());
        map.put("currency", schedule.price().currency());
        map.put("required", schedule.required());
        return map;
    }
}
