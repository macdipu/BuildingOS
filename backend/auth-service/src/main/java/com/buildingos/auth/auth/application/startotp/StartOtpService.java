package com.buildingos.auth.auth.application.startotp;

import com.buildingos.auth.auth.application.port.out.OtpProvider;
import com.buildingos.auth.auth.domain.model.PhoneNumber;
import com.buildingos.auth.auth.domain.repository.OtpChallengeRepository;
import java.time.Clock;
import java.time.Duration;

public final class StartOtpService implements StartOtpUseCase {
    public static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);
    private final OtpChallengeRepository repository;
    private final OtpProvider provider;
    private final Clock clock;
    private final Duration cooldown;
    private final int maxPerHour;

    public StartOtpService(OtpChallengeRepository repository, OtpProvider provider, Clock clock,
            Duration cooldown, int maxPerHour) {
        if (cooldown.isNegative() || cooldown.isZero() || maxPerHour < 1) {
            throw new IllegalArgumentException("OTP start limits must be positive");
        }
        this.repository = repository;
        this.provider = provider;
        this.clock = clock;
        this.cooldown = cooldown;
        this.maxPerHour = maxPerHour;
    }

    @Override
    public StartOtpResult execute(StartOtpCommand command) {
        var phone = PhoneNumber.parse(command.phone()).value();
        var now = clock.instant();
        var challenge = repository.start(phone, now, now.plus(CHALLENGE_TTL), cooldown, maxPerHour)
                .orElseThrow(OtpRateLimitedException::new);
        try {
            provider.issue(challenge);
        } catch (RuntimeException failure) {
            repository.consume(challenge.id(), clock.instant());
            // A vendor exception may contain the code/message. Never propagate it to logs.
            throw new IllegalStateException("OTP delivery failed");
        }
        return new StartOtpResult(challenge.id(), challenge.expiresAt());
    }
}
