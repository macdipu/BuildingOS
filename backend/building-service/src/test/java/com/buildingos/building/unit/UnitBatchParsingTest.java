package com.buildingos.building.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.buildingos.building.shared.domain.model.DomainRuleException;
import com.buildingos.building.unit.domain.model.InvalidUnitInputException;
import com.buildingos.building.unit.domain.model.NumberPattern;
import com.buildingos.building.unit.infrastructure.sheet.CsvUnitSheetParser;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

/** Batch number patterns and strict CSV parsing (UO-04, TECH-SPEC-F4 "Bulk validation"). */
class UnitBatchParsingTest {
    private final CsvUnitSheetParser csv = new CsvUnitSheetParser();

    private static byte[] bytes(String text) { return text.getBytes(StandardCharsets.UTF_8); }

    private static String code(Runnable action) {
        try {
            action.run();
        } catch (DomainRuleException rejected) {
            return rejected.code();
        }
        throw new AssertionError("expected rejection");
    }

    @Test
    void patternsCoverFloorLetterAndRanges() {
        assertThat(new NumberPattern("{floor}{letter}").apply(3, 1, 1)).isEqualTo("3B");
        assertThat(new NumberPattern("{floor}{nn}").apply(4, 0, 1)).isEqualTo("401");
        assertThat(new NumberPattern("P-{n}").apply(0, 4, 101)).isEqualTo("P-105");
        assertThatThrownBy(() -> new NumberPattern("{floor}")).isInstanceOf(InvalidUnitInputException.class);
        assertThatThrownBy(() -> new NumberPattern("{letter}").apply(1, 26, 1))
                .isInstanceOf(InvalidUnitInputException.class);
    }

    @Test
    void parsesQuotedCsvWithBomAndFlexibleHeaders() {
        var rows = csv.parse(bytes("﻿Number,Floor,Type,Area Sqft,Bedrooms,Default Maintenance Rate,Notes\r\n"
                + "1A,1st Floor,flat,1250.5,3,2500,\"corner, \"\"sunny\"\"\"\r\n\r\n"
                + "1B,1st Floor,FLAT,900,,,\n"), 500);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).rowNumber()).isEqualTo(1);
        assertThat(rows.get(0).floorLabel()).isEqualTo("1st Floor");
        assertThat(rows.get(0).notes()).isEqualTo("corner, \"sunny\"");
        assertThat(rows.get(1).rowNumber()).isEqualTo(3);
        assertThat(rows.get(1).bedrooms()).isNull();
    }

    @Test
    void rejectsFormulasMissingColumnsBadEncodingAndTooManyRows() {
        String header = "number,floor,type,areaSqft\n";
        assertThat(code(() -> csv.parse(bytes(header + "=HYPERLINK(\"x\"),1,FLAT,10\n"), 500)))
                .isEqualTo("SHEET_FORMULA_NOT_ALLOWED");
        assertThat(code(() -> csv.parse(bytes("number,floor,type\n1,1,FLAT\n"), 500)))
                .isEqualTo("SHEET_COLUMN_MISSING");
        assertThat(code(() -> csv.parse(new byte[] {(byte) 0xC3, (byte) 0x28}, 500)))
                .isEqualTo("SHEET_ENCODING_INVALID");
        assertThat(code(() -> csv.parse(bytes(header + "1,1,FLAT,10\n2,1,FLAT,10\n3,1,FLAT,10\n"), 2)))
                .isEqualTo("BATCH_TOO_LARGE");
        assertThat(code(() -> csv.parse(bytes(header + "\"1,1,FLAT,10\n"), 500))).isEqualTo("SHEET_QUOTE_UNCLOSED");
        assertThat(csv.supports("units.CSV", null)).isTrue();
        assertThat(csv.supports("units.xlsx", "application/octet-stream")).isFalse();
    }
}
