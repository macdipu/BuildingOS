package com.buildingos.building.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.building.unit.domain.model.FloorDetails;
import com.buildingos.building.unit.domain.model.FloorKind;
import com.buildingos.building.unit.domain.model.InvalidUnitInputException;
import com.buildingos.building.unit.domain.model.UnitDetails;
import com.buildingos.building.unit.domain.model.UnitType;
import java.math.BigDecimal;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/** UO-02/UO-D03 unit validation shared by individual and batch input. */
class UnitDetailsTest {
    private static final UUID FLOOR = UUID.randomUUID();

    private static UnitDetails unit(String number, String area) {
        return new UnitDetails(number, FLOOR, UnitType.FLAT, area == null ? null : new BigDecimal(area), 3,
                new BigDecimal("2500"), "  corner flat  ");
    }

    private static String code(Runnable action) {
        try {
            action.run();
        } catch (InvalidUnitInputException invalid) {
            return invalid.code() + ":" + invalid.field();
        }
        throw new AssertionError("expected rejection");
    }

    @Test
    void trimsNumberForDisplayAndComparesCaseInsensitively() {
        var details = unit("  4a ", "1250.5");
        assertThat(details.number()).isEqualTo("4a");
        assertThat(details.normalizedNumber()).isEqualTo(unit("4A", "1").normalizedNumber());
        assertThat(details.areaSqft()).isEqualByComparingTo("1250.50").hasToString("1250.50");
        assertThat(details.notes()).isEqualTo("corner flat");
        assertThat(unit("B-2 East", "1").normalizedNumber()).isEqualTo("B-2 EAST");
    }

    @Test
    void rejectsMissingOrInvalidFieldsWithFieldCodes() {
        assertThat(code(() -> unit(" ", "10"))).isEqualTo("UNIT_NUMBER_REQUIRED:number");
        assertThat(code(() -> unit("x".repeat(33), "10"))).isEqualTo("UNIT_NUMBER_TOO_LONG:number");
        assertThat(code(() -> unit("1", null))).isEqualTo("AREA_REQUIRED:areaSqft");
        assertThat(code(() -> unit("1", "0"))).isEqualTo("AREA_NOT_POSITIVE:areaSqft");
        assertThat(code(() -> unit("1", "-5"))).isEqualTo("AREA_NOT_POSITIVE:areaSqft");
        assertThat(code(() -> unit("1", "10.123"))).isEqualTo("AREA_PRECISION:areaSqft");
        assertThat(code(() -> unit("1", "12345678901"))).isEqualTo("AREA_PRECISION:areaSqft");
        assertThat(unit("1", "10.100").areaSqft()).hasToString("10.10");
        assertThat(code(() -> new UnitDetails("1", null, UnitType.FLAT, BigDecimal.TEN, null, null, null)))
                .isEqualTo("FLOOR_REQUIRED:floorId");
        assertThat(code(() -> new UnitDetails("1", FLOOR, null, BigDecimal.TEN, null, null, null)))
                .isEqualTo("UNIT_TYPE_REQUIRED:type");
        assertThat(code(() -> new UnitDetails("1", FLOOR, UnitType.FLAT, BigDecimal.TEN, -1, null, null)))
                .isEqualTo("BEDROOMS_INVALID:bedrooms");
        assertThat(code(() -> new UnitDetails("1", FLOOR, UnitType.FLAT, BigDecimal.TEN, null,
                new BigDecimal("-1"), null))).isEqualTo("MAINTENANCE_RATE_NEGATIVE:defaultMaintenanceRate");
        assertThat(code(() -> new UnitDetails("1", FLOOR, UnitType.FLAT, BigDecimal.TEN, null,
                new BigDecimal("1.001"), null))).isEqualTo("MAINTENANCE_RATE_PRECISION:defaultMaintenanceRate");
        assertThat(code(() -> new UnitDetails("1", FLOOR, UnitType.FLAT, BigDecimal.TEN, null, null,
                "n".repeat(1001)))).isEqualTo("NOTES_TOO_LONG:notes");
    }

    @Test
    void optionalFieldsMayBeAbsentAndZeroRateIsAllowed() {
        var details = new UnitDetails("P1", FLOOR, UnitType.PARKING, new BigDecimal("120"), null, BigDecimal.ZERO,
                "  ");
        assertThat(details.bedrooms()).isNull();
        assertThat(details.notes()).isNull();
        assertThat(details.defaultMaintenanceRate()).hasToString("0.00");
    }

    @Test
    void floorLabelIsRequiredAndNormalized() {
        assertThat(FloorDetails.of(" 1st floor ", FloorKind.REGULAR, 1).normalizedLabel()).isEqualTo("1ST FLOOR");
        assertThatThrownBy(() -> FloorDetails.of("", FloorKind.ROOF, 9)).isInstanceOf(InvalidUnitInputException.class);
        assertThatThrownBy(() -> FloorDetails.of("Roof", null, 9)).isInstanceOf(InvalidUnitInputException.class);
        assertThatThrownBy(() -> FloorDetails.of("Roof", FloorKind.ROOF, null))
                .isInstanceOf(InvalidUnitInputException.class);
    }
}
