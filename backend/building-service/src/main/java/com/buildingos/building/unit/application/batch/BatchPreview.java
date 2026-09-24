package com.buildingos.building.unit.application.batch;

import java.util.List;

public record BatchPreview(List<BatchRowResult> rows) {
    public BatchPreview {
        rows = List.copyOf(rows);
    }

    public boolean valid() { return rows.stream().allMatch(BatchRowResult::valid); }

    public boolean hasCode(String code) {
        return rows.stream().flatMap(r -> r.errors().stream()).anyMatch(e -> e.code().equals(code));
    }
}
