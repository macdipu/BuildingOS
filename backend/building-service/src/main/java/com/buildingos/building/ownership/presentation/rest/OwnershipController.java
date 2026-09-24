package com.buildingos.building.ownership.presentation.rest;

import com.buildingos.building.ownership.application.OwnershipResult;
import com.buildingos.building.ownership.application.assignownership.AssignOwnershipCommand;
import com.buildingos.building.ownership.application.assignownership.AssignOwnershipUseCase;
import com.buildingos.building.ownership.application.getcurrentownerships.GetCurrentOwnershipsQuery;
import com.buildingos.building.ownership.application.getcurrentownerships.GetCurrentOwnershipsUseCase;
import com.buildingos.building.ownership.application.getownershiphistory.GetOwnershipHistoryQuery;
import com.buildingos.building.ownership.application.getownershiphistory.GetOwnershipHistoryUseCase;
import com.buildingos.building.ownership.application.transferownership.TransferOwnershipCommand;
import com.buildingos.building.ownership.application.transferownership.TransferOwnershipUseCase;
import com.buildingos.building.ownership.presentation.rest.request.AssignOwnershipRequest;
import com.buildingos.building.ownership.presentation.rest.request.TransferOwnershipRequest;
import com.buildingos.building.ownership.presentation.rest.response.CurrentOwnershipResponse;
import com.buildingos.building.ownership.presentation.rest.response.OwnershipHistoryResponse;
import com.buildingos.building.ownership.presentation.rest.response.OwnershipResponse;
import com.buildingos.building.shared.presentation.rest.CurrentActor;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Unit ownership: assignment, transfer and history (UO-05..08). No generic PUT/DELETE of ownership exists. */
@RestController
@RequestMapping("/api/v1/buildings/{buildingId}/units/{unitId}")
public class OwnershipController {
    private final AssignOwnershipUseCase assign;
    private final TransferOwnershipUseCase transfer;
    private final GetOwnershipHistoryUseCase history;
    private final GetCurrentOwnershipsUseCase current;

    public OwnershipController(AssignOwnershipUseCase assign, TransferOwnershipUseCase transfer,
            GetOwnershipHistoryUseCase history, GetCurrentOwnershipsUseCase current) {
        this.assign = assign;
        this.transfer = transfer;
        this.history = history;
        this.current = current;
    }

    @PostMapping("/ownerships")
    public ResponseEntity<ApiEnvelope<OwnershipResponse>> assign(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID buildingId, @PathVariable UUID unitId, @RequestBody AssignOwnershipRequest body,
            HttpServletRequest request) {
        var result = assign.execute(CurrentActor.from(jwt), new AssignOwnershipCommand(buildingId, unitId,
                body.ownerUserId(), body.share(), body.effectiveDate(), body.notes(), body.reason(),
                body.expectedVersion(), body.operationId()));
        return reply(result, request);
    }

    @PostMapping("/ownership-transfers")
    public ResponseEntity<ApiEnvelope<OwnershipResponse>> transfer(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID buildingId, @PathVariable UUID unitId, @RequestBody TransferOwnershipRequest body,
            HttpServletRequest request) {
        var result = transfer.execute(CurrentActor.from(jwt), new TransferOwnershipCommand(buildingId, unitId,
                body.sourceOwnerUserId(), body.recipientUserId(), body.share(), body.effectiveDate(), body.reference(),
                body.reason(), body.expectedVersion(), body.operationId()));
        return reply(result, request);
    }

    @GetMapping("/ownerships")
    public ApiEnvelope<CurrentOwnershipResponse> current(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID buildingId, @PathVariable UUID unitId, HttpServletRequest request) {
        var result = current.execute(CurrentActor.from(jwt), new GetCurrentOwnershipsQuery(buildingId, unitId));
        return ApiEnvelope.of(CurrentOwnershipResponse.of(result), CorrelationFilter.traceId(request));
    }

    @GetMapping("/ownership-history")
    public ApiEnvelope<OwnershipHistoryResponse> history(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID buildingId, @PathVariable UUID unitId, HttpServletRequest request) {
        var result = history.execute(CurrentActor.from(jwt), new GetOwnershipHistoryQuery(buildingId, unitId));
        return ApiEnvelope.of(OwnershipHistoryResponse.of(result), CorrelationFilter.traceId(request));
    }

    private static ResponseEntity<ApiEnvelope<OwnershipResponse>> reply(OwnershipResult result,
            HttpServletRequest request) {
        return ResponseEntity.status(result.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                .body(ApiEnvelope.of(OwnershipResponse.of(result), CorrelationFilter.traceId(request)));
    }
}
