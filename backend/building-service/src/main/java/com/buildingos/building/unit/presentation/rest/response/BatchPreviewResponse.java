package com.buildingos.building.unit.presentation.rest.response;

import com.buildingos.building.unit.application.batch.BatchPreview;
import com.buildingos.building.unit.application.batch.BatchRowResult;
import com.buildingos.building.unit.application.batch.RowError;
import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/** Every submitted row, canonicalized when valid, with localizable field errors otherwise. */
public record BatchPreviewResponse(boolean valid, int rowCount, int invalidRowCount, List<Row> rows) {
    public record Row(int rowNumber, String number, UUID floorId, String floorLabel, String type, BigDecimal areaSqft,
            Integer bedrooms, BigDecimal defaultMaintenanceRate, String notes, List<RowError> errors) {
        static Row of(BatchRowResult r) {
            var in = r.input();
            String floorLabel = r.floor() == null ? in.floorLabel() : r.floor().details().label();
            UUID floorId = r.floor() == null ? in.floorId() : r.floor().id();
            if (r.details() == null) {
                return new Row(in.rowNumber(), in.number(), floorId, floorLabel, in.type(), decimal(in.areaSqft()),
                        integer(in.bedrooms()), decimal(in.defaultMaintenanceRate()), in.notes(), r.errors());
            }
            var d = r.details();
            return new Row(in.rowNumber(), d.number(), floorId, floorLabel, d.type().name(), d.areaSqft(), d.bedrooms(),
                    d.defaultMaintenanceRate(), d.notes(), List.of());
        }

        private static BigDecimal decimal(String raw) {
            try {
                return raw == null ? null : new BigDecimal(raw.strip());
            } catch (NumberFormatException echoedAsError) {
                return null;
            }
        }

        private static Integer integer(String raw) {
            try {
                return raw == null ? null : Integer.valueOf(raw.strip());
            } catch (NumberFormatException echoedAsError) {
                return null;
            }
        }
    }

    public static BatchPreviewResponse of(BatchPreview preview) {
        var rows = preview.rows().stream().map(Row::of).toList();
        return new BatchPreviewResponse(preview.valid(), rows.size(),
                (int) preview.rows().stream().filter(r -> !r.valid()).count(), rows);
    }
}
