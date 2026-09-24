package com.buildingos.building.shared.presentation.rest;

import com.buildingos.building.shared.application.BusinessRuleException;
import com.buildingos.building.shared.application.DependencyUnavailableException;
import com.buildingos.building.shared.application.NotPermittedException;
import com.buildingos.building.shared.domain.model.DomainRuleException;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** Maps building rule failures to {@link ApiError}; runs before platform-web's catch-all handler. */
@RestControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE)
public class BuildingErrorHandler {
    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ApiError> rule(BusinessRuleException rule, HttpServletRequest request) {
        HttpStatus status = switch (rule.kind()) {
            case NOT_FOUND -> HttpStatus.NOT_FOUND;
            case CONFLICT -> HttpStatus.CONFLICT;
        };
        return error(status, rule.code(), rule.getMessage(), request);
    }

    @ExceptionHandler(DomainRuleException.class)
    public ResponseEntity<ApiError> domain(DomainRuleException rule, HttpServletRequest request) {
        HttpStatus status = switch (rule.kind()) {
            case INVALID -> HttpStatus.BAD_REQUEST;
            case CONFLICT -> HttpStatus.CONFLICT;
            case TOO_LARGE -> HttpStatus.CONTENT_TOO_LARGE;
            case UNSUPPORTED_TYPE -> HttpStatus.UNSUPPORTED_MEDIA_TYPE;
        };
        return error(status, rule.code(), rule.getMessage(), request);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiError> tooLarge(HttpServletRequest request) {
        return error(HttpStatus.CONTENT_TOO_LARGE, "DOCUMENT_TOO_LARGE", "Document exceeds the size limit", request);
    }

    @ExceptionHandler(DependencyUnavailableException.class)
    public ResponseEntity<ApiError> dependencyDown(HttpServletRequest request) {
        return error(HttpStatus.SERVICE_UNAVAILABLE, "DEPENDENCY_UNAVAILABLE",
                "A required service is unavailable; nothing was changed", request);
    }

    @ExceptionHandler(NotPermittedException.class)
    public ResponseEntity<ApiError> notPermitted(HttpServletRequest request) {
        return error(HttpStatus.FORBIDDEN, "ACCESS_DENIED", "Access is denied", request);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> invalid(IllegalArgumentException invalid, HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", invalid.getMessage(), request);
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class, MissingServletRequestPartException.class,
            MultipartException.class})
    public ResponseEntity<ApiError> unreadable(HttpServletRequest request) {
        return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request failed validation", request);
    }

    private static ResponseEntity<ApiError> error(HttpStatus status, String code, String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status).body(ApiError.of(code, message, CorrelationFilter.traceId(request)));
    }
}
