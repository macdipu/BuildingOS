package com.buildingos.building.shared.domain.model;

public final class StaleVersionException extends DomainRuleException {
    public StaleVersionException(String entity) {
        super("STALE_VERSION", Kind.CONFLICT, entity + " changed; reload and retry");
    }
}
