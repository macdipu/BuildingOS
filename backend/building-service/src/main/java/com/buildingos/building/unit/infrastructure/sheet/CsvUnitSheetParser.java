package com.buildingos.building.unit.infrastructure.sheet;

import com.buildingos.building.unit.application.batch.BatchRowInput;
import com.buildingos.building.unit.application.port.out.UnitSheetParser;
import com.buildingos.building.unit.domain.model.BatchRejectedException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * RFC 4180 CSV as plain data: UTF-8 (BOM tolerated), quoted fields; rows are mapped by {@link SheetTable}.
 * Row numbers count data rows from 1.
 */
@Component
public class CsvUnitSheetParser implements UnitSheetParser {
    @Override
    public boolean supports(String filename, String contentType) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        return name.endsWith(".csv") || "text/csv".equalsIgnoreCase(contentType);
    }

    @Override
    public List<BatchRowInput> parse(byte[] content, int maxRows) {
        var records = records(decode(content), maxRows + 1);
        List<SheetTable.Record> numbered = new ArrayList<>();
        for (int i = 0; i < records.size(); i++) {
            numbered.add(new SheetTable.Record(i, records.get(i)));
        }
        return SheetTable.rows(numbered, maxRows);
    }

    private static String decode(byte[] content) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(content)).toString();
            return text.startsWith("\uFEFF") ? text.substring(1) : text;
        } catch (CharacterCodingException notUtf8) {
            throw BatchRejectedException.invalid("SHEET_ENCODING_INVALID", "The sheet must be UTF-8 text");
        }
    }

    /** Stops after {@code limit} records so an oversized sheet is rejected without parsing it fully. */
    private static List<List<String>> records(String text, int limit) {
        List<List<String>> records = new ArrayList<>();
        List<String> current = new ArrayList<>();
        var field = new StringBuilder();
        boolean quoted = false;
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            if (quoted) {
                if (c == '"' && i + 1 < text.length() && text.charAt(i + 1) == '"') {
                    field.append('"');
                    i++;
                } else if (c == '"') {
                    quoted = false;
                } else {
                    field.append(c);
                }
            } else if (c == '"' && field.isEmpty()) {
                quoted = true;
            } else if (c == ',') {
                current.add(field.toString());
                field.setLength(0);
            } else if (c == '\n' || c == '\r') {
                current.add(field.toString());
                field.setLength(0);
                records.add(current);
                current = new ArrayList<>();
                if (records.size() > limit) {
                    throw SheetTable.tooLarge(limit - 1);
                }
                if (c == '\r' && i + 1 < text.length() && text.charAt(i + 1) == '\n') {
                    i++;
                }
            } else {
                field.append(c);
            }
            i++;
        }
        if (quoted) {
            throw BatchRejectedException.invalid("SHEET_QUOTE_UNCLOSED", "The sheet has an unclosed quote");
        }
        if (!field.isEmpty() || !current.isEmpty()) {
            current.add(field.toString());
            records.add(current);
        }
        return records;
    }
}
