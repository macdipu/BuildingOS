package com.buildingos.building.duplicate.domain.model;

import com.buildingos.building.buildingapplication.domain.model.ApplicationDetails;
import com.buildingos.building.buildingapplication.domain.model.Coordinates;
import java.text.Normalizer;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Possible-duplicate review signals (BRD 149.6). Signals only: nothing is merged or blocked. Names and addresses are
 * compared after normalization (NFKC, case, punctuation, whitespace, configured filler words such as "tower").
 */
public final class DuplicateMatcher {
    private static final double EARTH_RADIUS_METERS = 6_371_000;
    private final Set<String> ignoredTokens;
    private final double radiusMeters;

    public DuplicateMatcher(Set<String> ignoredTokens, double radiusMeters) {
        if (radiusMeters <= 0) {
            throw new IllegalArgumentException("Duplicate radius must be positive");
        }
        this.ignoredTokens = ignoredTokens.stream().map(t -> t.toLowerCase(Locale.ROOT)).collect(Collectors.toSet());
        this.radiusMeters = radiusMeters;
    }

    public double radiusMeters() { return radiusMeters; }

    public List<DuplicateMatch> match(ApplicationDetails subject, List<DuplicateCandidate> candidates) {
        return candidates.stream()
                .map(candidate -> new DuplicateMatch(candidate, signals(subject, candidate)))
                .filter(match -> !match.signals().isEmpty())
                .toList();
    }

    private Set<DuplicateSignal> signals(ApplicationDetails subject, DuplicateCandidate candidate) {
        var signals = EnumSet.noneOf(DuplicateSignal.class);
        if (same(normalize(subject.buildingName()), normalize(candidate.name()))) {
            signals.add(DuplicateSignal.NAME);
        }
        if (same(normalize(subject.district()), normalize(candidate.district()))
                && same(normalize(join(subject.address(), subject.area())),
                        normalize(join(candidate.address(), candidate.area())))) {
            signals.add(DuplicateSignal.ADDRESS);
        }
        if (subject.contactPhone() != null && subject.contactPhone().value().equals(candidate.contactPhone())) {
            signals.add(DuplicateSignal.CONTACT_PHONE);
        }
        if (subject.coordinates() != null && candidate.coordinates() != null
                && distanceMeters(subject.coordinates(), candidate.coordinates()) <= radiusMeters) {
            signals.add(DuplicateSignal.COORDINATES);
        }
        return signals;
    }

    public String normalize(String value) {
        if (value == null) {
            return "";
        }
        String folded = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT)
                .replaceAll("[^\\p{L}\\p{M}\\p{N}]+", " ");
        return Arrays.stream(folded.trim().split(" "))
                .filter(token -> !token.isEmpty() && !ignoredTokens.contains(token))
                .collect(Collectors.joining(" "));
    }

    public static double distanceMeters(Coordinates a, Coordinates b) {
        double lat1 = Math.toRadians(a.latitude());
        double lat2 = Math.toRadians(b.latitude());
        double dLat = lat2 - lat1;
        double dLon = Math.toRadians(b.longitude() - a.longitude());
        double h = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return 2 * EARTH_RADIUS_METERS * Math.asin(Math.min(1, Math.sqrt(h)));
    }

    private static boolean same(String a, String b) { return !a.isEmpty() && a.equals(b); }

    private static String join(String a, String b) { return (a == null ? "" : a) + " " + (b == null ? "" : b); }
}
