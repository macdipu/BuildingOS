package com.buildingos.auth.auth.application.issueservicetoken;

/** Unknown client id or wrong secret; deliberately does not say which. */
public final class ServiceClientRejectedException extends RuntimeException {
    public ServiceClientRejectedException() { super("Invalid service client credentials"); }
}
