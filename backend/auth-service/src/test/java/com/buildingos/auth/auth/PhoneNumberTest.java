package com.buildingos.auth.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.auth.auth.domain.model.PhoneNumber;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;

class PhoneNumberTest {
    @ParameterizedTest
    @ValueSource(strings = {"01711234567", "8801711234567", "+8801711234567"})
    void acceptedFormsCanonicalizeToLocal(String raw) {
        assertThat(PhoneNumber.parse(raw).value()).isEqualTo("01711234567");
    }

    @ParameterizedTest
    @ValueSource(strings = {"01311234567", "01911234567"})
    void operatorRangeBoundariesAreAccepted(String raw) {
        assertThat(PhoneNumber.parse(raw).value()).isEqualTo(raw);
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"", "01211234567", "01011234567", "0171123456", "017112345678",
            "1711234567", "+880171123456", "880 1711234567", "01711-234567", " 01711234567",
            "+01711234567", "008801711234567", "0271234567", "abcdefghijk"})
    void rejectsEverythingElse(String raw) {
        assertThatThrownBy(() -> PhoneNumber.parse(raw)).isInstanceOf(IllegalArgumentException.class);
    }

    @ParameterizedTest
    @ValueSource(strings = {"8801711234567", "01211234567"})
    void constructorRequiresCanonicalForm(String raw) {
        assertThatThrownBy(() -> new PhoneNumber(raw)).isInstanceOf(IllegalArgumentException.class);
    }
}
