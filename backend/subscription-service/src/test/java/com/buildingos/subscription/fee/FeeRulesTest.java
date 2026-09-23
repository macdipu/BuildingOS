package com.buildingos.subscription.fee;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.subscription.fee.domain.model.FeeCode;
import com.buildingos.subscription.fee.domain.model.FeeSchedule;
import com.buildingos.subscription.fee.domain.model.FeeStatus;
import com.buildingos.subscription.fee.domain.model.Money;
import com.buildingos.subscription.fee.domain.model.PaymentMethod;
import com.buildingos.subscription.fee.domain.model.PaymentRecord;
import com.buildingos.subscription.fee.domain.model.PaymentReference;
import com.buildingos.subscription.fee.domain.model.ReferenceType;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class FeeRulesTest {
    private static final PaymentReference REF = new PaymentReference(ReferenceType.BUILDING_APPLICATION, UUID.randomUUID());

    private static FeeSchedule schedule(String amount, boolean required) {
        return new FeeSchedule(FeeCode.BUILDING_CREATION, new Money(new BigDecimal(amount), "BDT"), required, Instant.now());
    }

    private static PaymentRecord paid(String amount, String currency) {
        return new PaymentRecord(UUID.randomUUID(), FeeCode.BUILDING_CREATION, REF,
                new Money(new BigDecimal(amount), currency), PaymentMethod.MANUAL, "ref", LocalDate.now(),
                UUID.randomUUID(), Instant.now());
    }

    @Test
    void partialPaymentsSumToSettled() {
        var fee = schedule("5000.00", true);
        assertThat(FeeStatus.of(fee, List.of())).isEqualTo(FeeStatus.UNPAID);
        assertThat(FeeStatus.of(fee, List.of(paid("3000", "BDT")))).isEqualTo(FeeStatus.UNPAID);
        assertThat(FeeStatus.of(fee, List.of(paid("3000", "BDT"), paid("2000", "BDT")))).isEqualTo(FeeStatus.SETTLED);
    }

    @Test
    void otherCurrencyNeverCountsAndOptionalFeeIsNotRequired() {
        assertThat(FeeStatus.of(schedule("10", true), List.of(paid("100", "USD")))).isEqualTo(FeeStatus.UNPAID);
        assertThat(FeeStatus.of(schedule("10", false), List.of())).isEqualTo(FeeStatus.NOT_REQUIRED);
        assertThat(FeeStatus.of(schedule("0", true), List.of())).isEqualTo(FeeStatus.SETTLED);
    }

    @Test
    void moneyAndPaymentValidation() {
        assertThatThrownBy(() -> new Money(new BigDecimal("-1"), "BDT")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(new BigDecimal("1.001"), "BDT")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> new Money(BigDecimal.ONE, "bdt")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> paid("0", "BDT")).isInstanceOf(IllegalArgumentException.class);
    }
}
