package com.buildingos.subscription.shared.application;

public final class NotPermittedException extends RuntimeException {
    public NotPermittedException() { super("Caller lacks the required platform role"); }
}
