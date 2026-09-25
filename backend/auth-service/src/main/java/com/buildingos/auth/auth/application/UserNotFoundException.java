package com.buildingos.auth.auth.application;

public final class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() { super("User not found"); }
}
