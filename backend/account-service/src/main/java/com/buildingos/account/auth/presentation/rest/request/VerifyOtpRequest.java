package com.buildingos.account.auth.presentation.rest.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

public record VerifyOtpRequest(@NotNull UUID attemptId, @NotBlank String phone, @NotBlank String code) {}
