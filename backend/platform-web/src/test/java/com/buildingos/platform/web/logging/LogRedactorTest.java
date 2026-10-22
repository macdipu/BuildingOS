package com.buildingos.platform.web.logging;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

/** STORY-004 AC 3 / BRD §93-§94: each redaction pattern, and what must stay untouched. */
class LogRedactorTest {

    @Test
    void masksBangladeshiMobileNumbersKeepingLastTwoDigits() {
        assertThat(LogRedactor.redact("otp sent to 01712345678")).isEqualTo("otp sent to 01*******78");
        assertThat(LogRedactor.redact("phone=+8801912345601")).isEqualTo("phone=+8801*******01");
        assertThat(LogRedactor.redact("{\"phone\":\"01812345699\"}")).isEqualTo("{\"phone\":\"01*******99\"}");
    }

    @Test
    void masksJwtShapedTokens() {
        String jwt = "eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJ1c2VyLTEifQ.c2lnbmF0dXJlX2J5dGVz-_";
        assertThat(LogRedactor.redact("Authorization: Bearer " + jwt + " rejected"))
                .isEqualTo("Authorization: Bearer [jwt] rejected");
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', value = {
        "otp=123456 accepted|otp=[redacted] accepted",
        "OTP = 123456|OTP = [redacted]",
        "code=987654&next=1|code=[redacted]&next=1",
        "password=hunter2, user=u1|password=[redacted], user=u1",
        "client-secret: s3cr3t|client-secret: [redacted]",
        "token=abc.def|token=[redacted]",
        "refreshToken=r-123|refreshToken=[redacted]",
        "nid=1990123456789|nid=[redacted]",
        "nationalId=1234567890 ok|nationalId=[redacted] ok",
    })
    void redactsSensitiveKeyValuePairs(String input, String expected) {
        assertThat(LogRedactor.redact(input)).isEqualTo(expected);
    }

    @Test
    void redactsSensitiveJsonMembersCaseInsensitively() {
        assertThat(LogRedactor.redact("{\"otp\":\"123456\",\"Password\" : \"p\\\"w\",\"refreshtoken\":\"r1\",\"n\":1}"))
                .isEqualTo("{\"otp\":\"[redacted]\",\"Password\" : \"[redacted]\",\"refreshtoken\":\"[redacted]\",\"n\":1}");
    }

    @Test
    void stripsPresignedUrlQueryStrings() {
        String url = "https://s3.example.com/building-documents/app/doc.pdf?X-Amz-Algorithm=AWS4-HMAC-SHA256"
                + "&X-Amz-Credential=AKIA%2F20261022&X-Amz-Security-Token=abc&X-Amz-Signature=deadbeef";
        assertThat(LogRedactor.redact("download " + url + " issued"))
                .isEqualTo("download https://s3.example.com/building-documents/app/doc.pdf issued");
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "3f2b8c1e-9d4a-4b7e-8c2f-01712345678a",
        "user=0a1b2c3d-0171-4234-8567-017123456789",
        "buildingId=017123456789 unitCount=1500",
        "http_request method=GET status=200",
        "statusCode=500 errorCode=INVALID_REQUEST otpLength=6",
        "https://example.com/api/v1/buildings?page=2&size=20",
        "2026-10-22T10:01:12.345+06:00 eyJhbGciOiJSUzI1NiJ9",
    })
    void leavesOrdinaryIdentifiersAndTextUntouched(String text) {
        assertThat(LogRedactor.redact(text)).isEqualTo(text);
    }

    @Test
    void sensitiveMemberNameHidesTheWholeValue() {
        assertThat(LogRedactor.redact("password", "anything")).isEqualTo(LogRedactor.REDACTED);
        assertThat(LogRedactor.redact("NationalId", "1234")).isEqualTo(LogRedactor.REDACTED);
        assertThat(LogRedactor.redact("message", "user 01712345678")).isEqualTo("user 01*******78");
        assertThat(LogRedactor.redact("message", null)).isNull();
        assertThat(LogRedactor.redact(null)).isNull();
    }
}
