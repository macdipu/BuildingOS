package com.buildingos.subscription.fee.application.getfeestatus;

import com.buildingos.subscription.shared.application.Actor;

public interface GetFeeStatusUseCase {
    FeeStatusView execute(Actor actor, GetFeeStatusQuery query);
}
