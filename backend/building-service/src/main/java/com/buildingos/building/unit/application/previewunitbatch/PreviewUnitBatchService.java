package com.buildingos.building.unit.application.previewunitbatch;

import com.buildingos.building.unit.application.batch.BatchLimits;
import com.buildingos.building.unit.application.batch.BatchPreview;
import com.buildingos.building.unit.application.batch.BatchRowInput;
import com.buildingos.building.unit.application.batch.UnitBatchGenerator;
import com.buildingos.building.unit.application.batch.UnitBatchValidator;
import com.buildingos.building.unit.application.port.out.UnitSheetParser;
import com.buildingos.building.membership.application.BuildingAccess;
import com.buildingos.building.shared.application.Actor;
import com.buildingos.building.shared.application.port.out.UnitOfWork;
import com.buildingos.building.shared.domain.model.DomainRuleException;
import com.buildingos.building.unit.domain.model.BatchRejectedException;
import java.util.List;
import java.util.UUID;

/** Advisory preview (UO-04): shows every row with its errors; commit revalidates everything. */
public final class PreviewUnitBatchService implements PreviewUnitBatchUseCase {
    private final BuildingAccess access;
    private final UnitBatchValidator validator;
    private final UnitBatchGenerator generator;
    private final List<UnitSheetParser> parsers;
    private final BatchLimits limits;
    private final UnitOfWork unitOfWork;

    public PreviewUnitBatchService(BuildingAccess access, UnitBatchValidator validator, UnitBatchGenerator generator,
            List<UnitSheetParser> parsers, BatchLimits limits, UnitOfWork unitOfWork) {
        this.access = access;
        this.validator = validator;
        this.generator = generator;
        this.parsers = List.copyOf(parsers);
        this.limits = limits;
        this.unitOfWork = unitOfWork;
    }

    @Override
    public BatchPreview execute(Actor actor, PreviewUnitBatchCommand command) {
        return unitOfWork.inTransaction(() -> {
            access.requireAdminToRead(actor, command.buildingId());
            return validator.validate(command.buildingId(), rows(command.buildingId(), command.source()), limits);
        });
    }

    private List<BatchRowInput> rows(UUID buildingId, BatchSource source) {
        if (source instanceof BatchSource.Rows rows) {
            return rows.rows();
        }
        if (source instanceof BatchSource.Generated generated) {
            return generator.generate(buildingId, generated.spec(), limits);
        }
        return parse((BatchSource.Sheet) source);
    }

    private List<BatchRowInput> parse(BatchSource.Sheet sheet) {
        if (sheet.content().length > limits.maxSheetBytes()) {
            throw new BatchRejectedException("SHEET_TOO_LARGE", DomainRuleException.Kind.TOO_LARGE,
                    "Sheet exceeds " + limits.maxSheetBytes() + " bytes");
        }
        return parsers.stream().filter(p -> p.supports(sheet.filename(), sheet.contentType())).findFirst()
                .orElseThrow(() -> new BatchRejectedException("SHEET_TYPE_UNSUPPORTED",
                        DomainRuleException.Kind.UNSUPPORTED_TYPE, "Upload a CSV file"))
                .parse(sheet.content(), limits.maxRows());
    }
}
