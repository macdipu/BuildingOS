package com.buildingos.backoffice.shared.application;

/** Another service failed; the use case changed nothing (HTTP 503). */
public final class DependencyUnavailableException extends RuntimeException {
    public DependencyUnavailableException(String dependency, Throwable cause) {
        super(dependency + " is unavailable", cause);
    }
}
