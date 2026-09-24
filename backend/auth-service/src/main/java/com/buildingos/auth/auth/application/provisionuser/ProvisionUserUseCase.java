package com.buildingos.auth.auth.application.provisionuser;

/** Idempotently finds or creates the user with the given phone (D-29); grants no roles. */
public interface ProvisionUserUseCase {
    ProvisionUserResult execute(ProvisionUserCommand command);
}
