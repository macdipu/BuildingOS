package com.buildingos.account.auth.application.port.out;

import com.buildingos.account.auth.domain.model.OtpChallenge;

public interface OtpProvider {
    void issue(OtpChallenge challenge);
    boolean verify(OtpChallenge challenge, String code);
}
