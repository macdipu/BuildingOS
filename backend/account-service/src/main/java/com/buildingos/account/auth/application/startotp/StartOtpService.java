package com.buildingos.account.auth.application.startotp;

import com.buildingos.account.auth.domain.repository.OtpChallengeRepository;
import java.time.Clock;
import java.time.Duration;
import java.util.regex.Pattern;

public final class StartOtpService implements StartOtpUseCase {
    private static final Pattern PHONE_PATTERN = Pattern.compile("^\\+?[0-9]{7,15}$");
    public static final Duration CHALLENGE_TTL = Duration.ofMinutes(5);

    private final OtpChallengeRepository repository;
    private final Clock clock;

    public StartOtpService(OtpChallengeRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public StartOtpResult execute(StartOtpCommand command) {
        var phone = command.phone();
        if (phone == null || !PHONE_PATTERN.matcher(phone).matches()) {
            throw new IllegalArgumentException("Phone number must be digits, optionally starting with '+'");
        }
        var now = clock.instant();
        var challenge = repository.start(phone, now, now.plus(CHALLENGE_TTL));
        return new StartOtpResult(challenge.id(), challenge.expiresAt());
    }
}
