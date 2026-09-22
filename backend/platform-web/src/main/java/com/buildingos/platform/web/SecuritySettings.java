package com.buildingos.platform.web;

import jakarta.validation.constraints.NotBlank;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("platform.security")
public record SecuritySettings(@NotBlank String issuer, @NotBlank String audience,
        @NotBlank String jwkSetUri, List<String> allowedOrigins) {
    public SecuritySettings {
        allowedOrigins = allowedOrigins == null ? List.of() : List.copyOf(allowedOrigins);
    }
}
