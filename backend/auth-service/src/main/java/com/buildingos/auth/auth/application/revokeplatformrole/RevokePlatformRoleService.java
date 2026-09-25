package com.buildingos.auth.auth.application.revokeplatformrole;

import com.buildingos.auth.auth.application.PlatformRoleManagementNotPermittedException;
import com.buildingos.auth.auth.application.UserNotFoundException;
import com.buildingos.auth.auth.application.port.out.UnitOfWork;
import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.model.PlatformRoleAuditAction;
import com.buildingos.auth.auth.domain.model.PlatformRoleAuditEntry;
import com.buildingos.auth.auth.domain.repository.PlatformRoleAuditRepository;
import com.buildingos.auth.auth.domain.repository.UserRepository;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SUPER_ADMIN only (D-10); no caller may revoke their own platform role (extends ID-04). An effective revoke
 * appends one audit row in the same transaction (D-37); revoking a role not held writes no row.
 */
public final class RevokePlatformRoleService implements RevokePlatformRoleUseCase {
    private static final Logger log = LoggerFactory.getLogger(RevokePlatformRoleService.class);
    private final UserRepository users;
    private final PlatformRoleAuditRepository audits;
    private final UnitOfWork uow;
    private final Clock clock;

    public RevokePlatformRoleService(UserRepository users, PlatformRoleAuditRepository audits, UnitOfWork uow,
            Clock clock) {
        this.users = users;
        this.audits = audits;
        this.uow = uow;
        this.clock = clock;
    }

    @Override
    public void execute(RevokePlatformRoleCommand command) {
        if (!command.callerPlatformRoles().contains(PlatformRole.SUPER_ADMIN.name())) {
            throw new PlatformRoleManagementNotPermittedException();
        }
        if (command.callerUserId().equals(command.targetUserId())) {
            throw new PlatformRoleManagementNotPermittedException();
        }
        uow.inTransaction(() -> {
            if (users.findById(command.targetUserId()).isEmpty()) {
                throw new UserNotFoundException();
            }
            if (users.revokePlatformRole(command.targetUserId(), command.role())) {
                audits.append(new PlatformRoleAuditEntry(UUID.randomUUID(), command.callerUserId(),
                        command.targetUserId(), command.role(), PlatformRoleAuditAction.REVOKE, clock.instant()));
            }
            log.info("platform_role_revoked actor={} target={} role={}",
                    command.callerUserId(), command.targetUserId(), command.role());
            return null;
        });
    }
}
