package com.buildingos.auth.auth.infrastructure.security;

import com.buildingos.auth.auth.application.port.out.OtpProvider;
import com.buildingos.auth.auth.domain.model.OtpChallenge;

/** Constructed only by validated local/test provider selection. */
public final class DevelopmentOtpProvider implements OtpProvider {
    @Override public void issue(OtpChallenge challenge) { }
    @Override public boolean verify(OtpChallenge challenge, String code) { return "000000".equals(code); }
}
