package com.buildingos.auth.auth.presentation.rest.response;

public record ServiceTokenResponse(String accessToken, String tokenType, long expiresIn) {}
