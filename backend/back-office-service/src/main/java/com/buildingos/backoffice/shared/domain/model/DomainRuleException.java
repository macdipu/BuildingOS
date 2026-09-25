package com.buildingos.backoffice.shared.domain.model;

/** A domain rule a request broke, with a stable API error code. */
public abstract class DomainRuleException extends RuntimeException {
    public enum Kind { INVALID, CONFLICT }

    private final String code;
    private final Kind kind;

    protected DomainRuleException(String code, Kind kind, String message) {
        super(message);
        this.code = code;
        this.kind = kind;
    }

    public String code() { return code; }
    public Kind kind() { return kind; }
}
