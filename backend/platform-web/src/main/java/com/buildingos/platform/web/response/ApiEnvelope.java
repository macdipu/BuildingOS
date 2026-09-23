package com.buildingos.platform.web.response;

import java.util.Map;

public record ApiEnvelope<T>(boolean success, T data, Map<String, Object> meta, String traceId) {
    public static <T> ApiEnvelope<T> of(T data, String traceId) {
        return new ApiEnvelope<>(true, data, Map.of(), traceId);
    }
}
