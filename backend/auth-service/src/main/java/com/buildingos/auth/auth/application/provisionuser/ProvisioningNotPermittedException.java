package com.buildingos.auth.auth.application.provisionuser;

public final class ProvisioningNotPermittedException extends RuntimeException {
    public ProvisioningNotPermittedException() { super("Caller lacks the required platform role"); }
}
