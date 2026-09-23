package com.buildingos.platform.web.response;

public record ApiError(boolean success, String code, String message, String traceId) {
    public static ApiError of(String code, String message, String traceId) {
        return new ApiError(false, code, message, traceId);
    }
}
