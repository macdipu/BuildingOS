package com.buildingos.building.duplicate.presentation.rest;

import com.buildingos.building.duplicate.application.finddupsignals.FindDuplicateSignalsQuery;
import com.buildingos.building.duplicate.application.finddupsignals.FindDuplicateSignalsUseCase;
import com.buildingos.building.duplicate.presentation.rest.response.DuplicateMatchResponse;
import com.buildingos.building.shared.presentation.rest.CurrentActor;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DuplicateController {
    private final FindDuplicateSignalsUseCase find;

    public DuplicateController(FindDuplicateSignalsUseCase find) { this.find = find; }

    @GetMapping("/api/v1/platform/building-applications/{applicationId}/duplicates")
    public ApiEnvelope<List<DuplicateMatchResponse>> duplicates(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID applicationId, HttpServletRequest request) {
        return ApiEnvelope.of(find.execute(CurrentActor.from(jwt), new FindDuplicateSignalsQuery(applicationId))
                .stream().map(DuplicateMatchResponse::of).toList(), CorrelationFilter.traceId(request));
    }
}
