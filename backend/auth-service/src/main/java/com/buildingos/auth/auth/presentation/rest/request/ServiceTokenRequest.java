package com.buildingos.auth.auth.presentation.rest.request;

import jakarta.validation.constraints.NotBlank;

public record ServiceTokenRequest(@NotBlank String clientId, @NotBlank String clientSecret) {
    @Override
    public String toString() {
        return "ServiceTokenRequest[clientId=" + clientId + "]";
    }
}
