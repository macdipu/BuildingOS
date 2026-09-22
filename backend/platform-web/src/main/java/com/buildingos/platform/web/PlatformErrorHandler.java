package com.buildingos.platform.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
public class PlatformErrorHandler {
    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiError> unavailable(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.of("SERVICE_UNAVAILABLE", "Service temporarily unavailable",
                        CorrelationFilter.traceId(request)));
    }
}
