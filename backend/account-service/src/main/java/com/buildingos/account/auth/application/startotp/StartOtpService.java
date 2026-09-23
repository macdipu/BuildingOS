package com.buildingos.account.auth.application.startotp;

import com.buildingos.account.auth.application.port.out.OtpProvider;
import com.buildingos.account.auth.domain.repository.OtpChallengeRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.regex.Pattern;

public final class StartOtpService implements StartOtpUseCase {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{7,15}$");
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
        var phone = command.phone();
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Phone number must be digits, optionally starting with '+'");
        }
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
