package com.buildingos.account.auth.infrastructure.config;

import com.buildingos.account.auth.application.getpublickeys.GetPublicSigningKeysService;
import com.buildingos.account.auth.application.getpublickeys.GetPublicSigningKeysUseCase;
import com.buildingos.account.auth.application.port.out.OtpProvider;
import com.buildingos.account.auth.application.port.out.SigningKeyProvider;
import com.buildingos.account.auth.application.port.out.SmsSender;
import com.buildingos.account.auth.application.port.out.TokenIssuer;
import com.buildingos.account.auth.application.seedsuperadmin.SeedSuperAdminService;
import com.buildingos.account.auth.application.seedsuperadmin.SeedSuperAdminUseCase;
import com.buildingos.account.auth.application.startotp.StartOtpService;
import com.buildingos.account.auth.application.startotp.StartOtpUseCase;
import com.buildingos.account.auth.application.verifyotp.VerifyOtpService;
import com.buildingos.account.auth.application.verifyotp.VerifyOtpUseCase;
import com.buildingos.account.auth.domain.repository.OtpChallengeRepository;
import com.buildingos.account.auth.domain.repository.UserRepository;
import com.buildingos.account.auth.infrastructure.security.DevelopmentOtpProvider;
import com.buildingos.account.auth.infrastructure.security.HashedCodeOtpProvider;
import com.buildingos.account.auth.infrastructure.security.RsaJwtIssuer;
import com.buildingos.platform.web.security.SecuritySettings;
import java.time.Clock;
import java.time.Duration;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/** Wires auth only after validating provider and signing configuration. */
@Configuration(proxyBeanMethods = false)
public class AuthConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    StartOtpUseCase startOtp(OtpChallengeRepository repository, OtpProvider provider, Clock clock,
            Environment env) {
        return new StartOtpService(repository, provider, clock,
                Duration.parse(env.getProperty("buildingos.otp.cooldown", "PT60S")),
                env.getProperty("buildingos.otp.max-per-hour", Integer.class, 5));
    }

    @Bean
    VerifyOtpUseCase verifyOtp(OtpChallengeRepository challenges, OtpProvider codeVerifier,
            UserRepository users, TokenIssuer tokenIssuer, Clock clock) {
        return new VerifyOtpService(challenges, codeVerifier, users, tokenIssuer, clock);
    }

    @Bean
    GetPublicSigningKeysUseCase getPublicSigningKeys(SigningKeyProvider signingKeys) {
        return new GetPublicSigningKeysService(signingKeys);
    }

    @Bean
    @Profile({"local", "test"})
    SeedSuperAdminUseCase seedSuperAdmin(UserRepository users) {
        return new SeedSuperAdminService(users);
    }

    @Bean
    OtpProvider otpProvider(OtpChallengeRepository repository,
            ObjectProvider<SmsSender> sender,
            Environment env) {
        boolean development = env.acceptsProfiles(Profiles.of("local", "test"));
        String selection = env.getProperty("buildingos.otp.provider", development ? "development" : "sms");
        return switch (selection) {
            case "development" -> {
                if (!development) throw new IllegalArgumentException("Development OTP requires local/test profile");
                yield new DevelopmentOtpProvider();
            }
            case "sms" -> new HashedCodeOtpProvider(
                    repository, sender.getObject(), env.getProperty("buildingos.otp.code-pepper"));
            default -> throw new IllegalArgumentException("Unknown OTP provider");
        };
    }

    @Bean
    RsaJwtIssuer tokenIssuer(SecuritySettings settings, Clock clock,
            Environment env) {
        return new RsaJwtIssuer(settings, clock,
                env.getProperty("buildingos.account.signing-jwks-path", ""),
                env.getProperty("buildingos.account.signing-active-kid", ""),
                Duration.parse(env.getProperty("buildingos.account.access-token-ttl", "PT15M")),
                env.acceptsProfiles(Profiles.of("local", "test")));
    }
}
