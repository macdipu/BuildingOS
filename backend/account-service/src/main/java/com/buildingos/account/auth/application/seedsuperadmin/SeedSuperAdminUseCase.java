package com.buildingos.account.auth.application.seedsuperadmin;

/** Idempotently ensures the user with the given phone exists and holds SUPER_ADMIN. */
public interface SeedSuperAdminUseCase {
    void execute(SeedSuperAdminCommand command);
}
