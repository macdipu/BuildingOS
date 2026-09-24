package com.buildingos.building.unit.presentation.rest;

import com.buildingos.building.shared.presentation.rest.CurrentActor;
import com.buildingos.building.unit.application.batch.BatchRowInput;
import com.buildingos.building.unit.application.batch.GenerateSpec;
import com.buildingos.building.unit.application.commitunitbatch.CommitResult;
import com.buildingos.building.unit.application.commitunitbatch.CommitUnitBatchCommand;
import com.buildingos.building.unit.application.commitunitbatch.CommitUnitBatchUseCase;
import com.buildingos.building.unit.application.previewunitbatch.BatchSource;
import com.buildingos.building.unit.application.previewunitbatch.PreviewUnitBatchCommand;
import com.buildingos.building.unit.application.previewunitbatch.PreviewUnitBatchUseCase;
import com.buildingos.building.unit.presentation.rest.request.BatchCommitRequest;
import com.buildingos.building.unit.presentation.rest.request.BatchPreviewRequest;
import com.buildingos.building.unit.presentation.rest.request.BatchRowRequest;
import com.buildingos.building.unit.presentation.rest.response.BatchCommitResponse;
import com.buildingos.building.unit.presentation.rest.response.BatchPreviewResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.IntStream;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * Bulk unit setup (UO-04): preview from JSON rows, a generator or a CSV upload, then an all-or-nothing commit.
 * A rejected commit answers with every row's errors ({@code success=false}, meta code BATCH_INVALID/CONFLICT).
 */
@RestController
@RequestMapping("/api/v1/buildings/{buildingId}/unit-batches")
public class UnitBatchController {
    private final PreviewUnitBatchUseCase preview;
    private final CommitUnitBatchUseCase commit;

    public UnitBatchController(PreviewUnitBatchUseCase preview, CommitUnitBatchUseCase commit) {
        this.preview = preview;
        this.commit = commit;
    }

    @PostMapping(value = "/preview", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ApiEnvelope<BatchPreviewResponse> preview(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            @RequestBody BatchPreviewRequest body, HttpServletRequest request) {
        if ((body.rows() == null) == (body.generate() == null)) {
            throw new IllegalArgumentException("Provide exactly one of rows or generate");
        }
        BatchSource source = body.rows() != null ? new BatchSource.Rows(rows(body.rows())) : generated(body.generate());
        var result = preview.execute(CurrentActor.from(jwt), new PreviewUnitBatchCommand(buildingId, source));
        return ApiEnvelope.of(BatchPreviewResponse.of(result), CorrelationFilter.traceId(request));
    }

    @PostMapping(value = "/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiEnvelope<BatchPreviewResponse> previewSheet(@AuthenticationPrincipal Jwt jwt,
            @PathVariable UUID buildingId, @RequestPart("file") MultipartFile file, HttpServletRequest request)
            throws IOException {
        var source = new BatchSource.Sheet(file.getBytes(), file.getOriginalFilename(), file.getContentType());
        var result = preview.execute(CurrentActor.from(jwt), new PreviewUnitBatchCommand(buildingId, source));
        return ApiEnvelope.of(BatchPreviewResponse.of(result), CorrelationFilter.traceId(request));
    }

    @PostMapping("/commit")
    public ResponseEntity<ApiEnvelope<?>> commit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            @RequestBody BatchCommitRequest body, HttpServletRequest request) {
        var result = commit.execute(CurrentActor.from(jwt), new CommitUnitBatchCommand(buildingId,
                body.rows() == null ? List.of() : rows(body.rows()), body.operationId(), body.reason()));
        String trace = CorrelationFilter.traceId(request);
        if (result instanceof CommitResult.Committed committed) {
            return ResponseEntity.status(committed.replayed() ? HttpStatus.OK : HttpStatus.CREATED)
                    .body(ApiEnvelope.of(BatchCommitResponse.of(committed), trace));
        }
        var rejected = ((CommitResult.Rejected) result).preview();
        boolean conflict = rejected.hasCode("UNIT_NUMBER_TAKEN");
        return ResponseEntity.status(conflict ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST)
                .body(new ApiEnvelope<>(false, BatchPreviewResponse.of(rejected),
                        Map.of("code", conflict ? "BATCH_CONFLICT" : "BATCH_INVALID"), trace));
    }

    private static List<BatchRowInput> rows(List<BatchRowRequest> rows) {
        return IntStream.range(0, rows.size()).mapToObj(i -> {
            var r = rows.get(i);
            return new BatchRowInput(i + 1, r.number(), r.floorId(), r.floorLabel(), r.type(), text(r.areaSqft()),
                    r.bedrooms() == null ? null : r.bedrooms().toString(), text(r.defaultMaintenanceRate()), r.notes());
        }).toList();
    }

    private static BatchSource generated(BatchPreviewRequest.Generate g) {
        return new BatchSource.Generated(new GenerateSpec(g.floorIds(), g.unitsPerFloor(), g.numberPattern(),
                g.start(), g.type(), g.areaSqft(), g.bedrooms(), g.defaultMaintenanceRate(), g.templateFloorId()));
    }

    private static String text(BigDecimal value) {
        return value == null ? null : value.toPlainString();
    }
}
