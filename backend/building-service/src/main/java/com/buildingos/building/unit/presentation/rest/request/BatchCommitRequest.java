package com.buildingos.building.unit.presentation.rest.request;

import java.util.List;
import java.util.UUID;

public record BatchCommitRequest(List<BatchRowRequest> rows, UUID operationId, String reason) {}
