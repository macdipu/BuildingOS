package com.buildingos.account.auth.application.getpublickeys;

public interface GetPublicSigningKeysUseCase {
    GetPublicSigningKeysResult execute(GetPublicSigningKeysQuery query);
}
