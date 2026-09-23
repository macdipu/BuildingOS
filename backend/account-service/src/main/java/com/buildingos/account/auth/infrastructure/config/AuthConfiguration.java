package com.buildingos.account.auth.infrastructure.config;

import com.buildingos.account.auth.application.getpublickeys.GetPublicSigningKeysService;
import com.buildingos.account.auth.application.getpublickeys.GetPublicSigningKeysUseCase;
import com.buildingos.account.auth.application.port.out.OtpCodeVerifier;
import com.buildingos.account.auth.application.port.out.SigningKeyProvider;
import com.buildingos.account.auth.application.port.out.TokenIssuer;
import com.buildingos.account.auth.application.seedsuperadmin.SeedSuperAdminService;
import com.buildingos.account.auth.application.seedsuperadmin.SeedSuperAdminUseCase;
import com.buildingos.account.auth.application.startotp.StartOtpService;
import com.buildingos.account.auth.application.startotp.StartOtpUseCase;
import com.buildingos.account.auth.application.verifyotp.VerifyOtpService;
import com.buildingos.account.auth.application.verifyotp.VerifyOtpUseCase;
import com.buildingos.account.auth.domain.repository.OtpChallengeRepository;
import com.buildingos.account.auth.domain.repository.UserRepository;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * The whole phone+OTP auth feature is local/test only for now: it depends on the
 * {@code @Profile("local","test")}-gated {@link OtpCodeVerifier} and {@link TokenIssuer}
 * beans (see {@code DevelopmentOtpCodeVerifier}, {@code LocalRsaJwtIssuer}) because no
 * real SMS/OTP provider or production token-issuance strategy exists yet (tracked as
 * follow-up work, not invented here). Gating this configuration the same way keeps a
 * non-local deployment starting cleanly without this feature, instead of failing to boot.
 */
@Configuration(proxyBeanMethods = false)
@Profile({"local", "test"})
public class AuthConfiguration {
    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    StartOtpUseCase startOtp(OtpChallengeRepository repository, Clock clock) {
        return new StartOtpService(repository, clock);
    }

    @Bean
    VerifyOtpUseCase verifyOtp(OtpChallengeRepository challenges, OtpCodeVerifier codeVerifier,
            UserRepository users, TokenIssuer tokenIssuer, Clock clock) {
        return new VerifyOtpService(challenges, codeVerifier, users, tokenIssuer, clock);
    }

    @Bean
    GetPublicSigningKeysUseCase getPublicSigningKeys(SigningKeyProvider signingKeys) {
        return new GetPublicSigningKeysService(signingKeys);
    }

    @Bean
    SeedSuperAdminUseCase seedSuperAdmin(UserRepository users) {
        return new SeedSuperAdminService(users);
    }
}
