package com.buildingos.subscription.fee.application.updatefeeschedule;

import com.buildingos.subscription.fee.domain.model.FeeCode;
import com.buildingos.subscription.fee.domain.model.Money;

public record UpdateFeeScheduleCommand(FeeCode code, Money price, boolean required) {}
