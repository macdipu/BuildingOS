package com.buildingos.subscription.shared.application;

/** A rejected request with a stable API error code; presentation maps {@link Kind} to an HTTP status. */
public final class BusinessRuleException extends RuntimeException {
    public enum Kind { NOT_FOUND, CONFLICT, FORBIDDEN }

    private final String code;
    private final Kind kind;

    public BusinessRuleException(String code, Kind kind, String message) {
        super(message);
        this.code = code;
        this.kind = kind;
    }

    public String code() { return code; }
    public Kind kind() { return kind; }

    public static BusinessRuleException notFound(String code, String message) {
        return new BusinessRuleException(code, Kind.NOT_FOUND, message);
    }

    public static BusinessRuleException conflict(String code, String message) {
        return new BusinessRuleException(code, Kind.CONFLICT, message);
    }

    public static BusinessRuleException forbidden(String code, String message) {
        return new BusinessRuleException(code, Kind.FORBIDDEN, message);
    }
}
