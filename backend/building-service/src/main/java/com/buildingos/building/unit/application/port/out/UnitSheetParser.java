package com.buildingos.building.unit.application.port.out;

import com.buildingos.building.unit.application.batch.BatchRowInput;
import java.util.List;

/** Parses an uploaded unit sheet strictly as data; never evaluates formulas or macros. */
public interface UnitSheetParser {
    boolean supports(String filename, String contentType);
    List<BatchRowInput> parse(byte[] content, int maxRows);
}
