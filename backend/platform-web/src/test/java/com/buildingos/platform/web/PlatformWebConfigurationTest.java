package com.buildingos.platform.web;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** PF-06 fail-closed configuration: a non-local/test runtime must refuse an insecure issuer/JWKS scheme. */
class PlatformWebConfigurationTest {

    private final PlatformWebConfiguration config = new PlatformWebConfiguration();

    private SecuritySettings settings(String scheme) {
        return new SecuritySettings(scheme + "://issuer.example/realm", "aud", scheme + "://issuer.example/jwks", List.of());
    }

    @Test
    void refusesHttpIssuerOutsideLocalOrTestProfile() {
        assertThatThrownBy(() -> config.jwtDecoder(settings("http"), new MockEnvironment()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("HTTPS");
    }

    @Test
    void refusesHttpIssuerEvenWithUnrelatedProfileActive() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("staging");
        assertThatThrownBy(() -> config.jwtDecoder(settings("http"), environment))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void allowsHttpIssuerUnderLocalProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("local");
        assertThatCode(() -> config.jwtDecoder(settings("http"), environment)).doesNotThrowAnyException();
    }

    @Test
    void allowsHttpIssuerUnderTestProfile() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("test");
        assertThatCode(() -> config.jwtDecoder(settings("http"), environment)).doesNotThrowAnyException();
    }

    @Test
    void allowsHttpsIssuerWithNoActiveProfile() {
        assertThatCode(() -> config.jwtDecoder(settings("https"), new MockEnvironment())).doesNotThrowAnyException();
    }
}
