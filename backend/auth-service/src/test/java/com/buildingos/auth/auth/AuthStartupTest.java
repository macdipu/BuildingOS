package com.buildingos.auth.auth;

import com.buildingos.auth.auth.application.port.out.*;
import com.buildingos.auth.auth.domain.repository.*;
import com.buildingos.auth.auth.infrastructure.config.AuthConfiguration;
import com.buildingos.auth.auth.infrastructure.security.*;
import com.buildingos.platform.web.security.SecuritySettings;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.mock;

class AuthStartupTest {
    ApplicationContextRunner runner(boolean local) {
        return new ApplicationContextRunner().withUserConfiguration(AuthConfiguration.class)
                .withInitializer(context -> { if (local) context.getEnvironment().setActiveProfiles("test"); })
                .withBean(OtpChallengeRepository.class, () -> mock(OtpChallengeRepository.class))
                .withBean(UserRepository.class, () -> mock(UserRepository.class))
                .withBean(PlatformRoleAuditRepository.class, () -> mock(PlatformRoleAuditRepository.class))
                .withBean(UnitOfWork.class, () -> mock(UnitOfWork.class))
                .withBean(SecuritySettings.class, () -> new SecuritySettings("https://auth.example", "app",
                        "https://auth.example/.well-known/jwks.json", List.of(), List.of()));
    }

    @Test void localDefaultsToDevelopment() {
        runner(true).run(context -> {
            assertThat(context).hasNotFailed();
            assertThat(context.getBean(OtpProvider.class)).isInstanceOf(DevelopmentOtpProvider.class);
        });
    }

    @Test void refusesDevelopmentOutsideLocalAndUnknownProvider() {
        runner(false).withPropertyValues("buildingos.otp.provider=development")
                .run(context -> assertThat(context).getFailure().rootCause()
                        .hasMessage("Development OTP requires local/test profile"));
        runner(true).withPropertyValues("buildingos.otp.provider=unknown")
                .run(context -> assertThat(context).getFailure().rootCause()
                        .hasMessage("Unknown OTP provider"));
    }

    @Test void smsRequiresSenderAndStrongPepper() {
        runner(true).withPropertyValues("buildingos.otp.provider=sms",
                        "buildingos.otp.code-pepper=12345678901234567890123456789012")
                .run(context -> assertThat(context).hasFailed());
        for (String pepper : List.of("", "short")) {
            runner(true).withBean(SmsSender.class, () -> (phone, message) -> {})
                    .withPropertyValues("buildingos.otp.provider=sms", "buildingos.otp.code-pepper=" + pepper)
                    .run(context -> assertThat(context).hasFailed());
        }
        runner(true).withBean(SmsSender.class, () -> (phone, message) -> {})
                .withPropertyValues("buildingos.otp.provider=sms",
                        "buildingos.otp.code-pepper=12345678901234567890123456789012")
                .run(context -> {
                    assertThat(context).hasNotFailed();
                    assertThat(context.getBean(OtpProvider.class)).isInstanceOf(HashedCodeOtpProvider.class);
                });
    }

    @Test void nonLocalRequiresSigningConfigurationEvenWithValidSmsProvider() {
        runner(false).withBean(SmsSender.class, () -> (phone, message) -> {})
                .withPropertyValues("buildingos.otp.provider=sms",
                        "buildingos.otp.code-pepper=12345678901234567890123456789012")
                .run(context -> assertThat(context).hasFailed());
    }
}
