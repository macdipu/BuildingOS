package com.buildingos.building.unit.infrastructure.sheet;

import com.buildingos.building.unit.application.batch.BatchRowInput;
import com.buildingos.building.unit.application.port.out.UnitSheetParser;
import com.buildingos.building.unit.domain.model.BatchRejectedException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * RFC 4180 CSV as plain data: UTF-8 (BOM tolerated), quoted fields, header row mapped case-insensitively.
 * A cell starting with {@code =} is refused as a formula. Row numbers count data rows from 1.
 */
@Component
public class CsvUnitSheetParser implements UnitSheetParser {
    private static final List<String> REQUIRED = List.of("number", "floor", "type", "areasqft");

    @Override
    public boolean supports(String filename, String contentType) {
        String name = filename == null ? "" : filename.toLowerCase(Locale.ROOT);
        return name.endsWith(".csv") || "text/csv".equalsIgnoreCase(contentType);
    }

    @Override
    public List<BatchRowInput> parse(byte[] content, int maxRows) {
        var records = records(decode(content), maxRows + 1);
        if (records.isEmpty()) {
            throw BatchRejectedException.invalid("SHEET_HEADER_MISSING", "The sheet needs a header row");
        }
        Map<String, Integer> columns = header(records.get(0));
        List<BatchRowInput> rows = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            var cells = records.get(i);
            if (cells.stream().allMatch(String::isBlank)) {
                continue;
            }
            rows.add(new BatchRowInput(i, cell(cells, columns, "number", i), null, cell(cells, columns, "floor", i),
                    cell(cells, columns, "type", i), cell(cells, columns, "areasqft", i),
                    cell(cells, columns, "bedrooms", i), cell(cells, columns, "defaultmaintenancerate", i),
                    cell(cells, columns, "notes", i)));
        }
        if (rows.size() > maxRows) {
            throw BatchRejectedException.invalid("BATCH_TOO_LARGE", "A batch allows at most " + maxRows + " rows");
        }
        return rows;
    }

    private static String decode(byte[] content) {
        try {
            String text = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(content)).toString();
            return text.startsWith("﻿") ? text.substring(1) : text;
        } catch (CharacterCodingException notUtf8) {
            throw BatchRejectedException.invalid("SHEET_ENCODING_INVALID", "The sheet must be UTF-8 text");
        }
    }

    private static Map<String, Integer> header(List<String> cells) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < cells.size(); i++) {
            columns.putIfAbsent(cells.get(i).toLowerCase(Locale.ROOT).replaceAll("[^a-z]", ""), i);
        }
        for (String required : REQUIRED) {
            if (!columns.containsKey(required)) {
                throw BatchRejectedException.invalid("SHEET_COLUMN_MISSING", "Missing column: " + required);
            }
        }
        return columns;
    }

    private static String cell(List<String> cells, Map<String, Integer> columns, String column, int row) {
        Integer index = columns.get(column);
        if (index == null || index >= cells.size()) {
            return null;
        }
        String value = cells.get(index);
        if (value.strip().startsWith("=")) {
            throw BatchRejectedException.invalid("SHEET_FORMULA_NOT_ALLOWED",
                    "Row " + row + " column " + column + " contains a formula");
        }
        return value.isBlank() ? null : value;
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
                    throw BatchRejectedException.invalid("BATCH_TOO_LARGE",
                            "A batch allows at most " + (limit - 1) + " rows");
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
