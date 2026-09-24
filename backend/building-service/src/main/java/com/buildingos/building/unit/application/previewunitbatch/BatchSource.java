package com.buildingos.building.unit.application.previewunitbatch;

import com.buildingos.building.unit.application.batch.BatchRowInput;
import com.buildingos.building.unit.application.batch.GenerateSpec;
import java.util.List;

/** Where preview rows come from: reviewed JSON rows, a generator, or an uploaded sheet. */
public sealed interface BatchSource {
    record Rows(List<BatchRowInput> rows) implements BatchSource {
        public Rows {
            rows = List.copyOf(rows);
        }
    }

    record Generated(GenerateSpec spec) implements BatchSource {}

    record Sheet(byte[] content, String filename, String contentType) implements BatchSource {}
}
