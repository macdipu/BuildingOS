package com.buildingos.auth.auth.application.assignplatformrole;

import com.buildingos.auth.auth.application.PlatformRoleManagementNotPermittedException;
import com.buildingos.auth.auth.application.UserNotFoundException;
import com.buildingos.auth.auth.application.port.out.UnitOfWork;
import com.buildingos.auth.auth.domain.model.PlatformRole;
import com.buildingos.auth.auth.domain.model.PlatformRoleAuditAction;
import com.buildingos.auth.auth.domain.model.PlatformRoleAuditEntry;
import com.buildingos.auth.auth.domain.model.User;
import com.buildingos.auth.auth.domain.repository.PlatformRoleAuditRepository;
import com.buildingos.auth.auth.domain.repository.UserRepository;
import java.time.Clock;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * SUPER_ADMIN only (D-10); no caller may grant themselves a platform role (extends ID-04). An effective grant
 * appends one audit row in the same transaction (D-37); a repeated grant changes nothing and writes no row.
 */
public final class AssignPlatformRoleService implements AssignPlatformRoleUseCase {
    private static final Logger log = LoggerFactory.getLogger(AssignPlatformRoleService.class);
    private final UserRepository users;
    private final PlatformRoleAuditRepository audits;
    private final UnitOfWork uow;
    private final Clock clock;

    public AssignPlatformRoleService(UserRepository users, PlatformRoleAuditRepository audits, UnitOfWork uow,
            Clock clock) {
        this.users = users;
        this.audits = audits;
        this.uow = uow;
        this.clock = clock;
    }

    @Override
    public User execute(AssignPlatformRoleCommand command) {
        if (!command.callerPlatformRoles().contains(PlatformRole.SUPER_ADMIN.name())) {
            throw new PlatformRoleManagementNotPermittedException();
        }
        if (command.callerUserId().equals(command.targetUserId())) {
            throw new PlatformRoleManagementNotPermittedException();
        }
        return uow.inTransaction(() -> {
            if (users.findById(command.targetUserId()).isEmpty()) {
                throw new UserNotFoundException();
            }
            if (users.grantPlatformRole(command.targetUserId(), command.role())) {
                audits.append(new PlatformRoleAuditEntry(UUID.randomUUID(), command.callerUserId(),
                        command.targetUserId(), command.role(), PlatformRoleAuditAction.GRANT, clock.instant()));
            }
            log.info("platform_role_granted actor={} target={} role={}",
                    command.callerUserId(), command.targetUserId(), command.role());
            return users.findById(command.targetUserId()).orElseThrow(UserNotFoundException::new);
        });
    }
}
