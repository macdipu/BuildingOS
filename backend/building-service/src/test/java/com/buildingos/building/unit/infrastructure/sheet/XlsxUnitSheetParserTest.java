package com.buildingos.building.unit.infrastructure.sheet;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildingos.building.shared.domain.model.DomainRuleException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.Test;

/** F4-T3b built-in .xlsx reader: data only, first sheet, and hostile-workbook rejection. */
class XlsxUnitSheetParserTest {
    private static final String WORKBOOK = "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\""
            + " xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\"><sheets>"
            + "<sheet name=\"Units\" sheetId=\"1\" r:id=\"rId7\"/></sheets></workbook>";
    private static final String RELS = "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/"
            + "relationships\"><Relationship Id=\"rId7\" Target=\"worksheets/units.xml\" Type=\"worksheet\"/>"
            + "</Relationships>";
    private static final String SHARED = "<sst><si><t>number</t></si><si><t>floor</t></si><si><t>type</t></si>"
            + "<si><t>Area Sqft</t></si><si><t>1st Floor</t></si><si><r><t>FL</t></r><r><t>AT</t></r>"
            + "<rPh><t>x</t></rPh></si></sst>";
    private final XlsxUnitSheetParser parser = new XlsxUnitSheetParser();

    private static byte[] zip(Map<String, byte[]> parts) throws IOException {
        var out = new ByteArrayOutputStream();
        try (var zip = new ZipOutputStream(out)) {
            for (var part : parts.entrySet()) {
                zip.putNextEntry(new ZipEntry(part.getKey()));
                zip.write(part.getValue());
                zip.closeEntry();
            }
        }
        return out.toByteArray();
    }

    private static byte[] workbook(String sheetData) throws IOException {
        Map<String, byte[]> parts = new LinkedHashMap<>();
        parts.put("xl/workbook.xml", WORKBOOK.getBytes(StandardCharsets.UTF_8));
        parts.put("xl/_rels/workbook.xml.rels", RELS.getBytes(StandardCharsets.UTF_8));
        parts.put("xl/sharedStrings.xml", SHARED.getBytes(StandardCharsets.UTF_8));
        parts.put("xl/worksheets/units.xml", ("<worksheet><sheetData>" + sheetData + "</sheetData></worksheet>")
                .getBytes(StandardCharsets.UTF_8));
        return zip(parts);
    }

    private static final String HEADER = "<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c><c r=\"B1\" t=\"s\"><v>1</v></c>"
            + "<c r=\"C1\" t=\"s\"><v>2</v></c><c r=\"D1\" t=\"s\"><v>3</v></c></row>";

    private String code(byte[] content) {
        try {
            parser.parse(content, 500);
        } catch (DomainRuleException rejected) {
            return rejected.code();
        }
        throw new AssertionError("expected rejection");
    }

    @Test
    void readsFirstSheetThroughRelationshipsWithSharedAndInlineStrings() throws IOException {
        var rows = parser.parse(workbook(HEADER
                + "<row r=\"2\"><c r=\"A2\"><v>101</v></c><c r=\"B2\" t=\"s\"><v>4</v></c>"
                + "<c r=\"C2\" t=\"s\"><v>5</v></c><c r=\"D2\"><v>1250.5</v></c></row>"
                + "<row r=\"4\"><c r=\"A4\" t=\"inlineStr\"><is><t>1B</t></is></c><c r=\"B4\" t=\"s\"><v>4</v></c>"
                + "<c r=\"C4\" t=\"inlineStr\"><is><t>PARKING</t></is></c><c r=\"D4\"><v>120</v></c></row>"), 500);
        assertThat(rows).hasSize(2);
        assertThat(rows.get(0).rowNumber()).isEqualTo(1);
        assertThat(rows.get(0).number()).isEqualTo("101");
        assertThat(rows.get(0).floorLabel()).isEqualTo("1st Floor");
        assertThat(rows.get(0).type()).isEqualTo("FLAT");
        assertThat(rows.get(0).areaSqft()).isEqualTo("1250.5");
        assertThat(rows.get(1).rowNumber()).isEqualTo(3);
        assertThat(rows.get(1).type()).isEqualTo("PARKING");
    }

    @Test
    void refusesFormulasMacrosDtdsAndMissingColumns() throws IOException {
        assertThat(code(workbook(HEADER + "<row r=\"2\"><c r=\"A2\"><f>1+1</f><v>2</v></c></row>")))
                .isEqualTo("SHEET_FORMULA_NOT_ALLOWED");
        Map<String, byte[]> macro = new LinkedHashMap<>();
        macro.put("xl/workbook.xml", WORKBOOK.getBytes(StandardCharsets.UTF_8));
        macro.put("xl/vbaProject.bin", new byte[] {1, 2, 3});
        assertThat(code(zip(macro))).isEqualTo("SHEET_MACROS_NOT_ALLOWED");
        Map<String, byte[]> dtd = new LinkedHashMap<>();
        dtd.put("xl/workbook.xml", ("<!DOCTYPE w [<!ENTITY x SYSTEM \"file:///etc/passwd\">]>" + WORKBOOK)
                .getBytes(StandardCharsets.UTF_8));
        assertThat(code(zip(dtd))).isEqualTo("SHEET_INVALID");
        assertThat(code(workbook("<row r=\"1\"><c r=\"A1\" t=\"s\"><v>0</v></c></row>")))
                .isEqualTo("SHEET_COLUMN_MISSING");
        assertThat(code("not a zip".getBytes(StandardCharsets.UTF_8))).isEqualTo("SHEET_INVALID");
    }

    @Test
    void refusesDecompressionBombsAndTooManyPartsOrRows() throws IOException {
        Map<String, byte[]> bomb = new LinkedHashMap<>();
        bomb.put("xl/workbook.xml", WORKBOOK.getBytes(StandardCharsets.UTF_8));
        bomb.put("xl/media/padding.bin", new byte[20 * 1024 * 1024]);
        assertThat(code(zip(bomb))).isEqualTo("SHEET_TOO_LARGE");

        Map<String, byte[]> many = new LinkedHashMap<>();
        many.put("xl/workbook.xml", WORKBOOK.getBytes(StandardCharsets.UTF_8));
        for (int i = 0; i < 70; i++) {
            many.put("xl/extra" + i + ".xml", new byte[] {'<', 'a', '/', '>'});
        }
        assertThat(code(zip(many))).isEqualTo("SHEET_TOO_LARGE");

        var rows = new StringBuilder(HEADER);
        for (int r = 2; r <= 4; r++) {
            rows.append("<row r=\"").append(r).append("\"><c r=\"A").append(r).append("\"><v>").append(r)
                    .append("</v></c></row>");
        }
        try {
            parser.parse(workbook(rows.toString()), 2);
            throw new AssertionError("expected rejection");
        } catch (DomainRuleException rejected) {
            assertThat(rejected.code()).isEqualTo("BATCH_TOO_LARGE");
        }
    }
}
