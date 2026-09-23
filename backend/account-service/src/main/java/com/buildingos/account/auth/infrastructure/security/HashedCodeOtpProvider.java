package com.buildingos.account.auth.infrastructure.security;

import com.buildingos.account.auth.application.port.out.OtpProvider;
import com.buildingos.account.auth.application.port.out.SmsSender;
import com.buildingos.account.auth.domain.model.OtpChallenge;
import com.buildingos.account.auth.domain.repository.OtpChallengeRepository;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Objects;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

public final class HashedCodeOtpProvider implements OtpProvider {
    private final OtpChallengeRepository challenges;
    private final SmsSender sender;
    private final byte[] pepper;
    private final SecureRandom random = new SecureRandom();

    public HashedCodeOtpProvider(OtpChallengeRepository challenges, SmsSender sender, String pepper) {
        if (pepper == null || pepper.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("OTP pepper must contain at least 32 UTF-8 bytes");
        }
        this.challenges = challenges;
        this.sender = Objects.requireNonNull(sender, "SMS sender is required");
        this.pepper = pepper.getBytes(StandardCharsets.UTF_8);
    }

    @Override
    public void issue(OtpChallenge challenge) {
        String code = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        challenges.saveCodeHash(challenge.id(), hash(challenge, code));
        sender.send(challenge.phone(), "Your BuildingOS verification code is " + code);
    }

    @Override
    public boolean verify(OtpChallenge challenge, String code) {
        if (code == null || !code.matches("[0-9]{6}") || challenge.codeHash() == null) return false;
        return MessageDigest.isEqual(challenge.codeHash().getBytes(StandardCharsets.US_ASCII),
                hash(challenge, code).getBytes(StandardCharsets.US_ASCII));
    }

    private String hash(OtpChallenge challenge, String code) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(pepper, "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(
                    (challenge.id().toString() + code).getBytes(StandardCharsets.UTF_8)));
        } catch (GeneralSecurityException failure) {
            throw new IllegalStateException("OTP hashing unavailable");
        }
    }
}
