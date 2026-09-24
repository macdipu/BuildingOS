package com.buildingos.auth.auth.application.port.out;

/** Vendor boundary. Implementations must not log message bodies or silently retry delivery. */
public interface SmsSender {
    void send(String phone, String message);
}
