package com.buildingos.platform.web;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.ResourceAccessException;

@RestControllerAdvice
public class PlatformErrorHandler {
    private static final Logger log = LoggerFactory.getLogger(PlatformErrorHandler.class);

    @ExceptionHandler(ResourceAccessException.class)
    public ResponseEntity<ApiError> unavailable(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(ApiError.of("SERVICE_UNAVAILABLE", "Service temporarily unavailable",
                        CorrelationFilter.traceId(request)));
    }

    @ExceptionHandler({IllegalArgumentException.class, MethodArgumentNotValidException.class})
    public ResponseEntity<ApiError> badRequest(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(ApiError.of("INVALID_REQUEST", "Request failed validation",
                        CorrelationFilter.traceId(request)));
    }

    /**
     * Last-resort handler: without this, an unexpected exception falls through to the
     * servlet container's default {@code /error} dispatch, which the security filter
     * chain then rejects as unauthenticated — masking the real failure as a misleading
     * 401. Catching it here keeps the actual error visible (logged, correct 500 status).
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> unexpected(HttpServletRequest request, Exception exception) {
        String traceId = CorrelationFilter.traceId(request);
        log.error("Unhandled exception for traceId={}", traceId, exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ApiError.of("INTERNAL_ERROR", "An unexpected error occurred", traceId));
    }
}
