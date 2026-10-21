package com.buildingos.auth.auth.application.issueservicetoken;

public record IssuedServiceToken(String accessToken, long expiresInSeconds) {
    @Override
    public String toString() {
        return "IssuedServiceToken[expiresInSeconds=" + expiresInSeconds + "]";
    }
}
