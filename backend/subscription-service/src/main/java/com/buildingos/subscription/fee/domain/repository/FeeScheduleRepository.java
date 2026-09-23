package com.buildingos.subscription.fee.domain.repository;

import com.buildingos.subscription.fee.domain.model.FeeCode;
import com.buildingos.subscription.fee.domain.model.FeeSchedule;
import java.util.Optional;

public interface FeeScheduleRepository {
    Optional<FeeSchedule> find(FeeCode code);
    void save(FeeSchedule schedule);
}
