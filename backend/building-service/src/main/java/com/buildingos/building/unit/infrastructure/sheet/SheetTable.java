package com.buildingos.building.unit.infrastructure.sheet;

import com.buildingos.building.unit.application.batch.BatchRowInput;
import com.buildingos.building.unit.domain.model.BatchRejectedException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Maps parsed sheet records (first = header) to batch rows, identically for CSV and XLSX. Header names match
 * case-insensitively ignoring non-letters; a cell starting with {@code =} is refused as a formula.
 */
final class SheetTable {
    private static final List<String> REQUIRED = List.of("number", "floor", "type", "areasqft");

    /** One sheet line; {@code rowNumber} is the data-row number reported back to the user. */
    record Record(int rowNumber, List<String> cells) {}

    private SheetTable() {}

    static List<BatchRowInput> rows(List<Record> records, int maxRows) {
        if (records.isEmpty()) {
            throw BatchRejectedException.invalid("SHEET_HEADER_MISSING", "The sheet needs a header row");
        }
        Map<String, Integer> columns = header(records.get(0).cells());
        List<BatchRowInput> rows = new ArrayList<>();
        for (var record : records.subList(1, records.size())) {
            var cells = record.cells();
            if (cells.stream().allMatch(c -> c == null || c.isBlank())) {
                continue;
            }
            int row = record.rowNumber();
            rows.add(new BatchRowInput(row, cell(cells, columns, "number", row), null, cell(cells, columns, "floor", row),
                    cell(cells, columns, "type", row), cell(cells, columns, "areasqft", row),
                    cell(cells, columns, "bedrooms", row), cell(cells, columns, "defaultmaintenancerate", row),
                    cell(cells, columns, "notes", row)));
            if (rows.size() > maxRows) {
                throw tooLarge(maxRows);
            }
        }
        return rows;
    }

    static BatchRejectedException tooLarge(int maxRows) {
        return BatchRejectedException.invalid("BATCH_TOO_LARGE", "A batch allows at most " + maxRows + " rows");
    }

    private static Map<String, Integer> header(List<String> cells) {
        Map<String, Integer> columns = new HashMap<>();
        for (int i = 0; i < cells.size(); i++) {
            if (cells.get(i) != null) {
                columns.putIfAbsent(cells.get(i).toLowerCase(Locale.ROOT).replaceAll("[^a-z]", ""), i);
            }
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
        if (index == null || index >= cells.size() || cells.get(index) == null) {
            return null;
        }
        String value = cells.get(index);
        if (value.strip().startsWith("=")) {
            throw formula(row, column);
        }
        return value.isBlank() ? null : value;
    }

    static BatchRejectedException formula(int row, String column) {
        return BatchRejectedException.invalid("SHEET_FORMULA_NOT_ALLOWED",
                "Row " + row + " column " + column + " contains a formula");
    }
}
