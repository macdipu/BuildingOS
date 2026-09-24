package com.buildingos.building.membership.presentation.rest;

import com.buildingos.building.buildingapplication.domain.model.ContactPhone;
import com.buildingos.building.membership.application.VerifiedPhoneIdentity;
import com.buildingos.building.shared.application.NotPermittedException;
import com.buildingos.building.shared.presentation.rest.CurrentActor;
import org.springframework.security.oauth2.jwt.Jwt;

/** Maps auth-service's validated {@code sub} + canonical {@code phone} claims; a missing/invalid phone denies. */
final class VerifiedPhoneClaims {
    private VerifiedPhoneClaims() {}

    static VerifiedPhoneIdentity from(Jwt jwt) {
        var actor = CurrentActor.from(jwt);
        String phone = jwt.getClaimAsString("phone");
        try {
            return new VerifiedPhoneIdentity(actor.userId(), new ContactPhone(phone));
        } catch (IllegalArgumentException notVerified) {
            throw new NotPermittedException();
        }
    }
}
