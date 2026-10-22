package com.buildingos.platform.web.logging;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Masks secrets and personal data in log text (BRD §93, §94; STORY-004 AC 3). Applied to every string
 * member of a structured log line by {@link RedactingJsonMembersCustomizer}.
 *
 * <ul>
 *   <li>presigned S3 URLs ({@code X-Amz-Signature}) lose their query string;</li>
 *   <li>JWT-shaped tokens become {@code [jwt]};</li>
 *   <li>values of {@code otp|code|password|secret|token|refreshToken|nid|nationalId} in {@code key=value} and
 *       {@code "key":"value"} form become {@code [redacted]};</li>
 *   <li>Bangladeshi mobile numbers keep their prefix and last two digits: {@code 01*******NN}.</li>
 * </ul>
 */
public final class LogRedactor {
    public static final String REDACTED = "[redacted]";
    public static final String JWT = "[jwt]";

    private static final String KEYS = "otp|code|password|secret|token|refreshToken|nid|nationalId";
    private static final Set<String> SENSITIVE_KEYS = Set.of(KEYS.toLowerCase(Locale.ROOT).split("\\|"));

    private static final Pattern PRESIGNED_URL =
            Pattern.compile("(?i)(https?://[^\\s\"'?\\\\]+)\\?[^\\s\"'\\\\]*x-amz-signature[^\\s\"'\\\\]*");
    private static final Pattern JWT_TOKEN =
            Pattern.compile("eyJ[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+\\.[A-Za-z0-9_-]+");
    private static final Pattern KEY_VALUE = Pattern.compile("(?i)(?<![A-Za-z0-9_])([\"']?)(" + KEYS + ")\\1"
            + "(\\s*[:=]\\s*)(\"(?:[^\"\\\\]|\\\\.)*\"|'[^']*'|[^\\s,;&)}\\]\"']+)");
    private static final Pattern BD_PHONE =
            Pattern.compile("(?<![A-Za-z0-9])(\\+?8801|01)\\d{7}(\\d{2})(?![A-Za-z0-9])");

    private LogRedactor() {
    }

    /** Redacts free text; {@code null} stays {@code null}. */
    public static String redact(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        String out = PRESIGNED_URL.matcher(text).replaceAll("$1");
        out = JWT_TOKEN.matcher(out).replaceAll(Matcher.quoteReplacement(JWT));
        out = KEY_VALUE.matcher(out).replaceAll(m -> {
            String value = m.group(4);
            char first = value.charAt(0);
            String masked = first == '"' || first == '\'' ? first + REDACTED + first : REDACTED;
            return Matcher.quoteReplacement(m.group(1) + m.group(2) + m.group(1) + m.group(3) + masked);
        });
        return BD_PHONE.matcher(out).replaceAll("$1*******$2");
    }

    /** Redacts a structured member: a sensitive member name hides the whole value, otherwise the text is redacted. */
    public static String redact(String memberName, String value) {
        if (value != null && memberName != null && SENSITIVE_KEYS.contains(memberName.toLowerCase(Locale.ROOT))) {
            return REDACTED;
        }
        return redact(value);
    }
}
