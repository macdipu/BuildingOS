package com.buildingos.building.membership.presentation.rest;

import com.buildingos.building.membership.application.inviteowner.InviteOwnerCommand;
import com.buildingos.building.membership.application.inviteowner.InviteOwnerUseCase;
import com.buildingos.building.membership.application.listinvitations.ListInvitationsQuery;
import com.buildingos.building.membership.application.listinvitations.ListInvitationsUseCase;
import com.buildingos.building.membership.application.listmembers.ListMembersQuery;
import com.buildingos.building.membership.application.listmembers.ListMembersUseCase;
import com.buildingos.building.membership.application.revokeinvitation.RevokeInvitationCommand;
import com.buildingos.building.membership.application.revokeinvitation.RevokeInvitationUseCase;
import com.buildingos.building.membership.application.revokemembership.RevokeMembershipCommand;
import com.buildingos.building.membership.application.revokemembership.RevokeMembershipUseCase;
import com.buildingos.building.membership.presentation.rest.request.InviteOwnerRequest;
import com.buildingos.building.membership.presentation.rest.request.MembershipReasonRequest;
import com.buildingos.building.membership.presentation.rest.request.RevokeMembershipRequest;
import com.buildingos.building.membership.presentation.rest.response.InvitationResponse;
import com.buildingos.building.membership.presentation.rest.response.MemberResponse;
import com.buildingos.building.shared.application.Page;
import com.buildingos.building.shared.presentation.rest.CurrentActor;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Building admin (or platform admin) membership and OWNER invitations; access is checked per request (UO-01). */
@RestController
@RequestMapping("/api/v1/buildings/{buildingId}")
public class BuildingMembershipController {
    private final InviteOwnerUseCase invite;
    private final ListInvitationsUseCase listInvitations;
    private final RevokeInvitationUseCase revokeInvitation;
    private final ListMembersUseCase listMembers;
    private final RevokeMembershipUseCase revokeMembership;

    public BuildingMembershipController(InviteOwnerUseCase invite, ListInvitationsUseCase listInvitations,
            RevokeInvitationUseCase revokeInvitation, ListMembersUseCase listMembers,
            RevokeMembershipUseCase revokeMembership) {
        this.invite = invite;
        this.listInvitations = listInvitations;
        this.revokeInvitation = revokeInvitation;
        this.listMembers = listMembers;
        this.revokeMembership = revokeMembership;
    }

    @GetMapping("/invitations")
    public ApiEnvelope<List<InvitationResponse>> invitations(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID buildingId, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size, HttpServletRequest request) {
        var result = listInvitations.execute(CurrentActor.from(jwt), new ListInvitationsQuery(buildingId, page, size));
        return paged(result, InvitationResponse::of, request);
    }

    @PostMapping("/invitations")
    public ResponseEntity<ApiEnvelope<InvitationResponse>> invite(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID buildingId, @RequestBody InviteOwnerRequest body, HttpServletRequest request) {
        if (!"OWNER".equals(body.role())) {
            throw new IllegalArgumentException("role must be OWNER");
        }
        var result = invite.execute(CurrentActor.from(jwt), new InviteOwnerCommand(buildingId, body.phone(),
                body.reason()));
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(ApiEnvelope.of(InvitationResponse.of(result.invitation()), CorrelationFilter.traceId(request)));
    }

    @PostMapping("/invitations/{invitationId}/revoke")
    public ApiEnvelope<InvitationResponse> revokeInvitation(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID buildingId, @PathVariable UUID invitationId, @RequestBody MembershipReasonRequest body,
            HttpServletRequest request) {
        var view = revokeInvitation.execute(CurrentActor.from(jwt),
                new RevokeInvitationCommand(buildingId, invitationId, body.reason()));
        return ApiEnvelope.of(InvitationResponse.of(view), CorrelationFilter.traceId(request));
    }

    @GetMapping("/members")
    public ApiEnvelope<List<MemberResponse>> members(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        var result = listMembers.execute(CurrentActor.from(jwt), new ListMembersQuery(buildingId, page, size));
        return paged(result, MemberResponse::of, request);
    }

    @PostMapping("/members/{membershipId}/revoke")
    public ApiEnvelope<MemberResponse> revokeMembership(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID buildingId, @PathVariable UUID membershipId, @RequestBody RevokeMembershipRequest body,
            HttpServletRequest request) {
        var revoked = revokeMembership.execute(CurrentActor.from(jwt),
                new RevokeMembershipCommand(buildingId, membershipId, body.reason(), body.expectedVersion()));
        return ApiEnvelope.of(MemberResponse.of(revoked), CorrelationFilter.traceId(request));
    }

    private static <T, R> ApiEnvelope<List<R>> paged(Page<T> page, Function<T, R> mapper, HttpServletRequest request) {
        return new ApiEnvelope<>(true, page.items().stream().map(mapper).toList(),
                Map.of("page", page.page(), "size", page.size(), "total", page.total()),
                CorrelationFilter.traceId(request));
    }
}
