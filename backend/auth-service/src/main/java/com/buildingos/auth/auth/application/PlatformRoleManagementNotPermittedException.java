package com.buildingos.auth.auth.application;

public final class PlatformRoleManagementNotPermittedException extends RuntimeException {
    public PlatformRoleManagementNotPermittedException() { super("Caller lacks the required platform role"); }
}
