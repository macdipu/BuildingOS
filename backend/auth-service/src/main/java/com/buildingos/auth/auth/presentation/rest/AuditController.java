package com.buildingos.auth.auth.presentation.rest;

import com.buildingos.auth.auth.application.PlatformRoleManagementNotPermittedException;
import com.buildingos.auth.auth.application.listauditevents.ListAuditEventsQuery;
import com.buildingos.auth.auth.application.listauditevents.ListAuditEventsUseCase;
import com.buildingos.auth.auth.presentation.rest.response.AuditEventResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.platform.web.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** F6-T5b common internal audit read (D-37). Service-to-service only: the caller relays a SUPER_ADMIN token. */
@RestController
public class AuditController {
    private final ListAuditEventsUseCase list;

    public AuditController(ListAuditEventsUseCase list) {
        this.list = list;
    }

    @ExceptionHandler(PlatformRoleManagementNotPermittedException.class)
    public ResponseEntity<ApiError> notPermitted(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(ApiError.of("ACCESS_DENIED", "Access is denied", CorrelationFilter.traceId(request)));
    }

    @GetMapping("/internal/audit")
    public ApiEnvelope<List<AuditEventResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String actorUserId,
            @RequestParam(required = false) String limit,
            HttpServletRequest request) {
        var events = list.execute(new ListAuditEventsQuery(callerRoles(jwt), instant("since", since),
                instant("until", until), blankToNull(entityType), uuid(actorUserId), integer(limit)));
        return ApiEnvelope.of(events.stream().map(AuditEventResponse::of).toList(),
                CorrelationFilter.traceId(request));
    }

    private static Set<String> callerRoles(Jwt jwt) {
        List<String> roles = jwt.getClaimAsStringList("platform_roles");
        return roles == null ? Set.of() : Set.copyOf(roles);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static Instant instant(String name, String value) {
        if (blankToNull(value) == null) {
            return null;
        }
        try {
            return Instant.parse(value);
        } catch (DateTimeParseException invalid) {
            throw new IllegalArgumentException(name + " must be an ISO-8601 instant");
        }
    }

    private static UUID uuid(String value) {
        if (blankToNull(value) == null) {
            return null;
        }
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException invalid) {
            throw new IllegalArgumentException("actorUserId must be a UUID");
        }
    }

    private static Integer integer(String value) {
        if (blankToNull(value) == null) {
            return null;
        }
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException invalid) {
            throw new IllegalArgumentException("limit must be an integer");
        }
    }
}
