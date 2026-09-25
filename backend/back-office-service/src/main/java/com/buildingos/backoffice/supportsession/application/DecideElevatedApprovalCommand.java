package com.buildingos.backoffice.supportsession.application;

import java.util.UUID;

/** Approve (reason optional) or deny (reason required) one elevated approval request. */
public record DecideElevatedApprovalCommand(UUID approvalId, String reason) {}
