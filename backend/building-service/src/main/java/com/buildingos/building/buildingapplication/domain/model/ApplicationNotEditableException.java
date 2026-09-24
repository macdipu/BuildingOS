package com.buildingos.building.buildingapplication.domain.model;

import com.buildingos.building.shared.domain.model.DomainRuleException;

public final class ApplicationNotEditableException extends DomainRuleException {
    public ApplicationNotEditableException(ApplicationStatus status) {
        super("NOT_EDITABLE", Kind.CONFLICT, "Application cannot be changed in status " + status.name());
    }
}
