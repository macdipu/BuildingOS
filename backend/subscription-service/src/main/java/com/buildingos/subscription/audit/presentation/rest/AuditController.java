package com.buildingos.subscription.audit.presentation.rest;

import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.subscription.audit.application.listauditevents.ListAuditEventsQuery;
import com.buildingos.subscription.audit.application.listauditevents.ListAuditEventsUseCase;
import com.buildingos.subscription.audit.presentation.rest.response.AuditEventResponse;
import com.buildingos.subscription.shared.presentation.rest.CurrentActor;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** F6-T5c common internal audit read (D-37). Service-to-service only: the caller relays a SUPER_ADMIN token. */
@RestController
public class AuditController {
    private final ListAuditEventsUseCase list;

    public AuditController(ListAuditEventsUseCase list) {
        this.list = list;
    }

    @GetMapping("/internal/audit")
    public ApiEnvelope<List<AuditEventResponse>> list(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(required = false) String since,
            @RequestParam(required = false) String until,
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) String actorUserId,
            @RequestParam(required = false) String limit,
            HttpServletRequest request) {
        var events = list.execute(CurrentActor.from(jwt), new ListAuditEventsQuery(instant("since", since),
                instant("until", until), blankToNull(entityType), uuid(actorUserId), integer(limit)));
        return ApiEnvelope.of(events.stream().map(AuditEventResponse::of).toList(),
                CorrelationFilter.traceId(request));
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
