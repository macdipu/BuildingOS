package com.buildingos.auth.auth.application.getpublickeys;

import com.buildingos.auth.auth.application.port.out.SigningKeyProvider;

public final class GetPublicSigningKeysService implements GetPublicSigningKeysUseCase {
    private final SigningKeyProvider signingKeys;

    public GetPublicSigningKeysService(SigningKeyProvider signingKeys) {
        this.signingKeys = signingKeys;
    }

    @Override
    public GetPublicSigningKeysResult execute(GetPublicSigningKeysQuery query) {
        return new GetPublicSigningKeysResult(signingKeys.publicSigningKeys());
    }
}
