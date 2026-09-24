package com.buildingos.auth.auth.presentation.rest.response;

public record VerifyOtpResponse(String accessToken, String tokenType, long expiresInSeconds, VerifiedUserResponse user) {}
