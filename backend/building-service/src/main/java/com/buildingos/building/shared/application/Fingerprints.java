package com.buildingos.building.shared.application;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.Objects;
import java.util.stream.Collectors;

/** Canonical SHA-256 request fingerprint for idempotent operations (TECH-SPEC-F4 "Idempotency"). */
public final class Fingerprints {
    private Fingerprints() {}

    public static String of(Object... parts) {
        String canonical = Arrays.stream(parts).map(p -> Objects.toString(p, "")).collect(Collectors.joining("\u001f"));
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(canonical.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }
}
