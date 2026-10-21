package com.buildingos.auth.auth.presentation.rest;

import com.buildingos.auth.auth.application.issueservicetoken.IssueServiceTokenCommand;
import com.buildingos.auth.auth.application.issueservicetoken.IssueServiceTokenUseCase;
import com.buildingos.auth.auth.application.issueservicetoken.ServiceClientRejectedException;
import com.buildingos.auth.auth.presentation.rest.request.ServiceTokenRequest;
import com.buildingos.auth.auth.presentation.rest.response.ServiceTokenResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import com.buildingos.platform.web.response.ApiError;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** Service-to-service only (D-38): not routed by the gateway. Client credentials in, short-lived service token out. */
@RestController
public class ServiceTokenController {
    private final IssueServiceTokenUseCase issue;

    public ServiceTokenController(IssueServiceTokenUseCase issue) {
        this.issue = issue;
    }

    @ExceptionHandler(ServiceClientRejectedException.class)
    public ResponseEntity<ApiError> rejected(HttpServletRequest request) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(ApiError.of("INVALID_CLIENT", "Invalid client credentials", CorrelationFilter.traceId(request)));
    }

    @PostMapping("/internal/service-tokens")
    public ApiEnvelope<ServiceTokenResponse> issue(@Valid @RequestBody ServiceTokenRequest body,
            HttpServletRequest request) {
        var token = issue.execute(new IssueServiceTokenCommand(body.clientId(), body.clientSecret()));
        return ApiEnvelope.of(new ServiceTokenResponse(token.accessToken(), "Bearer", token.expiresInSeconds()),
                CorrelationFilter.traceId(request));
    }
}
