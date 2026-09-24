package com.buildingos.auth.auth.application.port.out;

import com.buildingos.auth.auth.domain.model.OtpChallenge;

public interface OtpProvider {
    void issue(OtpChallenge challenge);
    boolean verify(OtpChallenge challenge, String code);
}
