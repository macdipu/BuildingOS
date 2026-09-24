package com.buildingos.auth.auth.application.getpublickeys;

public interface GetPublicSigningKeysUseCase {
    GetPublicSigningKeysResult execute(GetPublicSigningKeysQuery query);
}
