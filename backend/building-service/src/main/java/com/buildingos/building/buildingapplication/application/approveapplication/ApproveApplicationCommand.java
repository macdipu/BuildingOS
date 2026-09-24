package com.buildingos.building.buildingapplication.application.approveapplication;

import java.util.UUID;

/** {@code adminPhone}: initial BUILDING_ADMIN chosen by the approver; the review screen pre-fills the applicant (D-12). */
public record ApproveApplicationCommand(UUID applicationId, String adminPhone, String reason) {}
