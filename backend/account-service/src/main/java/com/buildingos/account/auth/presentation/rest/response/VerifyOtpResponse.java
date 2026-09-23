package com.buildingos.account.auth.presentation.rest.response;

public record VerifyOtpResponse(String accessToken, String tokenType, long expiresInSeconds, VerifiedUserResponse user) {}
