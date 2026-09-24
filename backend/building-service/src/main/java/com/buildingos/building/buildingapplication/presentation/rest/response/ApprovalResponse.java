package com.buildingos.building.buildingapplication.presentation.rest.response;

import com.buildingos.building.buildingapplication.application.approveapplication.ApprovalResult;
import java.util.UUID;

public record ApprovalResponse(ApplicationResponse application, UUID buildingId, String buildingStatus) {
    public static ApprovalResponse of(ApprovalResult result) {
        return new ApprovalResponse(ApplicationResponse.of(result.application(), true), result.building().id(),
                result.building().status().name());
    }
}
