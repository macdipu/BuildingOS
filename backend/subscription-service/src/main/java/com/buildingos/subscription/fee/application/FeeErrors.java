package com.buildingos.subscription.fee.application;

import com.buildingos.subscription.fee.domain.model.FeeCode;
import com.buildingos.subscription.shared.application.BusinessRuleException;

public final class FeeErrors {
    private FeeErrors() {}

    /** Fail closed: an unconfigured fee blocks, it never defaults to free (D-24). */
    public static BusinessRuleException notConfigured(FeeCode code) {
        return BusinessRuleException.conflict("FEE_NOT_CONFIGURED", code + " fee has not been configured");
    }
}
