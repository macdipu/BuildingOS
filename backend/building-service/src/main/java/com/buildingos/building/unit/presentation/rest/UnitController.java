package com.buildingos.building.unit.presentation.rest;

import com.buildingos.building.shared.application.Page;
import com.buildingos.building.shared.presentation.rest.CurrentActor;
import com.buildingos.building.shared.presentation.rest.Enums;
import com.buildingos.building.unit.application.FloorInput;
import com.buildingos.building.unit.application.UnitInput;
import com.buildingos.building.unit.application.createfloor.CreateFloorCommand;
import com.buildingos.building.unit.application.createfloor.CreateFloorUseCase;
import com.buildingos.building.unit.application.createunit.CreateUnitCommand;
import com.buildingos.building.unit.application.createunit.CreateUnitUseCase;
import com.buildingos.building.unit.application.getunit.GetUnitQuery;
import com.buildingos.building.unit.application.getunit.GetUnitUseCase;
import com.buildingos.building.unit.application.listfloors.ListFloorsQuery;
import com.buildingos.building.unit.application.listfloors.ListFloorsUseCase;
import com.buildingos.building.unit.application.listunits.ListUnitsQuery;
import com.buildingos.building.unit.application.listunits.ListUnitsUseCase;
import com.buildingos.building.unit.application.updatefloor.UpdateFloorCommand;
import com.buildingos.building.unit.application.updatefloor.UpdateFloorUseCase;
import com.buildingos.building.unit.application.updateunit.UpdateUnitCommand;
import com.buildingos.building.unit.application.updateunit.UpdateUnitUseCase;
import com.buildingos.building.unit.domain.model.FloorKind;
import com.buildingos.building.unit.domain.model.UnitType;
import com.buildingos.building.unit.domain.model.UnitSearch;
import com.buildingos.building.unit.presentation.rest.request.FloorRequest;
import com.buildingos.building.unit.presentation.rest.request.UnitRequest;
import com.buildingos.building.unit.presentation.rest.response.FloorResponse;
import com.buildingos.building.unit.presentation.rest.response.UnitResponse;
import com.buildingos.platform.web.correlation.CorrelationFilter;
import com.buildingos.platform.web.response.ApiEnvelope;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Floors and individual units of one building (UO-02/03); access is rechecked per request. */
@RestController
@RequestMapping("/api/v1/buildings/{buildingId}")
public class UnitController {
    private final CreateFloorUseCase createFloor;
    private final UpdateFloorUseCase updateFloor;
    private final ListFloorsUseCase listFloors;
    private final CreateUnitUseCase createUnit;
    private final UpdateUnitUseCase updateUnit;
    private final ListUnitsUseCase listUnits;
    private final GetUnitUseCase getUnit;

    public UnitController(CreateFloorUseCase createFloor, UpdateFloorUseCase updateFloor, ListFloorsUseCase listFloors,
            CreateUnitUseCase createUnit, UpdateUnitUseCase updateUnit, ListUnitsUseCase listUnits,
            GetUnitUseCase getUnit) {
        this.createFloor = createFloor;
        this.updateFloor = updateFloor;
        this.listFloors = listFloors;
        this.createUnit = createUnit;
        this.updateUnit = updateUnit;
        this.listUnits = listUnits;
        this.getUnit = getUnit;
    }

    @GetMapping("/floors")
    public ApiEnvelope<List<FloorResponse>> floors(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        return paged(listFloors.execute(CurrentActor.from(jwt), new ListFloorsQuery(buildingId, page, size)),
                FloorResponse::of, request);
    }

    @PostMapping("/floors")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<FloorResponse> createFloor(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            @RequestBody FloorRequest body, HttpServletRequest request) {
        var floor = createFloor.execute(CurrentActor.from(jwt),
                new CreateFloorCommand(buildingId, floorInput(body), body.reason()));
        return ApiEnvelope.of(FloorResponse.of(floor), CorrelationFilter.traceId(request));
    }

    @PutMapping("/floors/{floorId}")
    public ApiEnvelope<FloorResponse> updateFloor(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            @PathVariable UUID floorId, @RequestBody FloorRequest body, HttpServletRequest request) {
        var floor = updateFloor.execute(CurrentActor.from(jwt), new UpdateFloorCommand(buildingId, floorId,
                floorInput(body), body.expectedVersion(), body.reason()));
        return ApiEnvelope.of(FloorResponse.of(floor), CorrelationFilter.traceId(request));
    }

    @GetMapping("/units")
    public ApiEnvelope<List<UnitResponse>> units(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            @RequestParam(required = false) UUID floorId, @RequestParam(required = false) String type,
            @RequestParam(required = false) UUID ownerUserId, @RequestParam(required = false) String q,
            @RequestParam(required = false) String sort,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            HttpServletRequest request) {
        var search = new UnitSearch(floorId, Enums.parseOptional(UnitType.class, type, "type"), ownerUserId,
                UnitSortParam.numberQuery(q), UnitSortParam.parse(sort));
        var query = new ListUnitsQuery(buildingId, search, page, size);
        return paged(listUnits.execute(CurrentActor.from(jwt), query), UnitResponse::of, request);
    }

    @PostMapping("/units")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiEnvelope<UnitResponse> createUnit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            @RequestBody UnitRequest body, HttpServletRequest request) {
        var unit = createUnit.execute(CurrentActor.from(jwt),
                new CreateUnitCommand(buildingId, unitInput(body), body.reason()));
        return ApiEnvelope.of(UnitResponse.of(unit), CorrelationFilter.traceId(request));
    }

    @GetMapping("/units/{unitId}")
    public ApiEnvelope<UnitResponse> unit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            @PathVariable UUID unitId, HttpServletRequest request) {
        var unit = getUnit.execute(CurrentActor.from(jwt), new GetUnitQuery(buildingId, unitId));
        return ApiEnvelope.of(UnitResponse.of(unit), CorrelationFilter.traceId(request));
    }

    @PutMapping("/units/{unitId}")
    public ApiEnvelope<UnitResponse> updateUnit(@AuthenticationPrincipal Jwt jwt, @PathVariable UUID buildingId,
            @PathVariable UUID unitId, @RequestBody UnitRequest body, HttpServletRequest request) {
        var unit = updateUnit.execute(CurrentActor.from(jwt), new UpdateUnitCommand(buildingId, unitId,
                unitInput(body), body.expectedVersion(), body.reason()));
        return ApiEnvelope.of(UnitResponse.of(unit), CorrelationFilter.traceId(request));
    }

    private static FloorInput floorInput(FloorRequest body) {
        return new FloorInput(body.label(), Enums.parseOptional(FloorKind.class, body.kind(), "kind"),
                body.displayOrder());
    }

    private static UnitInput unitInput(UnitRequest body) {
        return new UnitInput(body.number(), body.floorId(), Enums.parseOptional(UnitType.class, body.type(), "type"),
                body.areaSqft(), body.bedrooms(), body.defaultMaintenanceRate(), body.notes());
    }

    private static <T, R> ApiEnvelope<List<R>> paged(Page<T> page, Function<T, R> mapper, HttpServletRequest request) {
        return new ApiEnvelope<>(true, page.items().stream().map(mapper).toList(),
                Map.of("page", page.page(), "size", page.size(), "total", page.total()),
                CorrelationFilter.traceId(request));
    }
}
