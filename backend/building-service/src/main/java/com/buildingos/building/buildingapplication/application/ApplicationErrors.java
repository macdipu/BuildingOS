package com.buildingos.building.buildingapplication.application;

import com.buildingos.building.shared.application.BusinessRuleException;
import java.util.UUID;

public final class ApplicationErrors {
    private ApplicationErrors() {}

    public static BusinessRuleException notFound(UUID id) {
        return BusinessRuleException.notFound("APPLICATION_NOT_FOUND", "Application " + id + " does not exist");
    }
}
