package com.buildingos.building.membership.infrastructure.config;

import com.buildingos.building.building.domain.repository.BuildingMembershipRepository;
import com.buildingos.building.building.domain.repository.BuildingRepository;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.membership.application.claiminvitation.ClaimInvitationService;
import com.buildingos.building.membership.application.claiminvitation.ClaimInvitationUseCase;
import com.buildingos.building.membership.application.inviteowner.InviteOwnerService;
import com.buildingos.building.membership.application.inviteowner.InviteOwnerUseCase;
import com.buildingos.building.membership.application.listinvitations.ListInvitationsService;
import com.buildingos.building.membership.application.listinvitations.ListInvitationsUseCase;
import com.buildingos.building.membership.application.listmembers.ListMembersService;
import com.buildingos.building.membership.application.listmembers.ListMembersUseCase;
import com.buildingos.building.membership.application.listmyinvitations.ListMyInvitationsService;
import com.buildingos.building.membership.application.listmyinvitations.ListMyInvitationsUseCase;
import com.buildingos.building.membership.application.revokeinvitation.RevokeInvitationService;
import com.buildingos.building.membership.application.revokeinvitation.RevokeInvitationUseCase;
import com.buildingos.building.membership.application.revokemembership.RevokeMembershipService;
import com.buildingos.building.membership.application.revokemembership.RevokeMembershipUseCase;
import com.buildingos.building.membership.domain.repository.BuildingInvitationRepository;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.repository.AuditRepository;
import com.buildingos.building.shared.domain.repository.OperationRepository;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(MembershipProperties.class)
public class MembershipConfiguration {
    @Bean
    BuildingAccess buildingAccess(BuildingRepository buildings, BuildingMembershipRepository memberships) {
        return new BuildingAccess(buildings, memberships);
    }

    @Bean
    InviteOwnerUseCase inviteOwner(BuildingAccess access, BuildingInvitationRepository invitations,
            AuditRepository audit, UnitOfWork uow, Clock clock, MembershipProperties properties) {
        return new InviteOwnerService(access, invitations, audit, uow, clock, properties.invitationTtl());
    }

    @Bean
    ListInvitationsUseCase listInvitations(BuildingAccess access, BuildingInvitationRepository invitations,
            UnitOfWork uow, Clock clock) {
        return new ListInvitationsService(access, invitations, uow, clock);
    }

    @Bean
    RevokeInvitationUseCase revokeInvitation(BuildingAccess access, BuildingInvitationRepository invitations,
            AuditRepository audit, UnitOfWork uow, Clock clock) {
        return new RevokeInvitationService(access, invitations, audit, uow, clock);
    }

    @Bean
    ListMyInvitationsUseCase listMyInvitations(BuildingInvitationRepository invitations, BuildingRepository buildings,
            Clock clock) {
        return new ListMyInvitationsService(invitations, buildings, clock);
    }

    @Bean
    ClaimInvitationUseCase claimInvitation(BuildingInvitationRepository invitations, BuildingRepository buildings,
            BuildingMembershipRepository memberships, OperationRepository operations, AuditRepository audit,
            UnitOfWork uow, Clock clock) {
        return new ClaimInvitationService(invitations, buildings, memberships, operations, audit, uow, clock);
    }

    @Bean
    ListMembersUseCase listMembers(BuildingAccess access, BuildingMembershipRepository memberships, UnitOfWork uow) {
        return new ListMembersService(access, memberships, uow);
    }

    @Bean
    RevokeMembershipUseCase revokeMembership(BuildingAccess access, BuildingMembershipRepository memberships,
            AuditRepository audit, UnitOfWork uow, Clock clock) {
        return new RevokeMembershipService(access, memberships, audit, uow, clock);
    }
}
