package com.buildingos.subscription.fee.presentation.rest.request;

import java.math.BigDecimal;

public record FeeScheduleRequest(BigDecimal amount, String currency, Boolean required) {}
