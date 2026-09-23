package com.buildingos.account.auth.presentation.rest.request;

import jakarta.validation.constraints.NotBlank;

public record StartOtpRequest(@NotBlank String phone) {}
