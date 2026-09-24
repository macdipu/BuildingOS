package com.buildingos.building.membership.presentation.rest;

import com.buildingos.building.membership.application.claiminvitation.ClaimInvitationCommand;
import com.buildingos.building.membership.application.claiminvitation.ClaimInvitationUseCase;
import com.buildingos.building.membership.application.listmyinvitations.ListMyInvitationsUseCase;
import com.buildingos.building.membership.presentation.rest.request.ClaimInvitationRequest;
import com.buildingos.building.membership.presentation.rest.response.MemberResponse;
import com.buildingos.building.membership.presentation.rest.response.MyInvitationResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Invitations addressed to the caller's verified phone (UO-01/05). */
@RestController
@RequestMapping("/api/v1/me/building-invitations")
public class MyInvitationController {
    private final ListMyInvitationsUseCase list;
    private final ClaimInvitationUseCase claim;

    public MyInvitationController(ListMyInvitationsUseCase list, ClaimInvitationUseCase claim) {
        this.list = list;
        this.claim = claim;
    }

    @GetMapping
    public ApiEnvelope<List<MyInvitationResponse>> list(@AuthenticationPrincipal Jwt jwt,
            HttpServletRequest request) {
        return ApiEnvelope.of(list.execute(VerifiedPhoneClaims.from(jwt)).stream().map(MyInvitationResponse::of)
                .toList(), CorrelationFilter.traceId(request));
    }

    @PostMapping("/{invitationId}/claim")
    public ApiEnvelope<MemberResponse> claim(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID invitationId,
            @RequestBody ClaimInvitationRequest body, HttpServletRequest request) {
        var membership = claim.execute(VerifiedPhoneClaims.from(jwt),
                new ClaimInvitationCommand(invitationId, body.operationId()));
        return ApiEnvelope.of(MemberResponse.of(membership), CorrelationFilter.traceId(request));
    }
}
