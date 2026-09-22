package com.buildingos.identity.auth.infrastructure;

import com.buildingos.identity.auth.application.port.in.StartOtpChallenge;
import com.buildingos.identity.auth.application.port.in.VerifyOtpChallenge;
import com.buildingos.identity.auth.application.port.out.OtpChallengeRepository;
import com.buildingos.identity.auth.application.port.out.OtpCodeVerifier;
import com.buildingos.identity.auth.application.port.out.TokenIssuer;
import com.buildingos.identity.auth.application.port.out.UserRepository;
import com.buildingos.identity.auth.application.usecase.StartOtpChallengeService;
import com.buildingos.identity.auth.application.usecase.VerifyOtpChallengeService;
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
    StartOtpChallenge startOtpChallenge(OtpChallengeRepository repository, Clock clock) {
        return new StartOtpChallengeService(repository, clock);
    }

    @Bean
    VerifyOtpChallenge verifyOtpChallenge(OtpChallengeRepository challenges, OtpCodeVerifier codeVerifier,
            UserRepository users, TokenIssuer tokenIssuer, Clock clock) {
        return new VerifyOtpChallengeService(challenges, codeVerifier, users, tokenIssuer, clock);
    }
}
